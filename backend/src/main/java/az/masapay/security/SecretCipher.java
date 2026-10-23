package az.masapay.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts restaurant secrets (Payriff secret key, iiko API login) before they reach the database
 * (#157), so a leaked dump or backup does not leak every restaurant's payment keys.
 * <p>
 * AES-256-GCM with a fresh random 12-byte IV per value. A stored value reads
 * {@code enc:<key id>:<base64(iv | ciphertext | tag)>}; the key id lets an old key keep decrypting
 * while new values use the current one (rotation). A value without the {@code enc:} prefix is
 * clear text from before encryption and is returned as is until it is re-encrypted.
 * <p>
 * The key comes from {@code MASAPAY_SECRETS_ENCRYPTION_KEY} (base64 of 32 bytes). Without it the
 * application refuses to start, except in local development, which uses a public dev key.
 */
@Component
public class SecretCipher {

	static final String PREFIX = "enc:";
	static final String DEV_KEY_ID = "dev";
	/** Public, for local development only; values encrypted with it are not secret. */
	static final byte[] DEV_KEY = "masapay-dev-only-secrets-key-32b".getBytes(StandardCharsets.US_ASCII);

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private static final int KEY_BYTES = 32;

	private final Map<String, SecretKey> keys;
	private final String currentKeyId;
	private final SecureRandom random = new SecureRandom();

	@Autowired
	public SecretCipher(@Value("${masapay.secrets.encryption-key:}") String encryptionKey,
			@Value("${masapay.secrets.key-id:k1}") String keyId,
			@Value("${masapay.secrets.old-keys:}") String oldKeys,
			@Value("${masapay.security.allow-dev-secrets:false}") boolean allowDevSecrets) {
		Map<String, SecretKey> all = new LinkedHashMap<>();
		if (allowDevSecrets) {
			all.put(DEV_KEY_ID, new SecretKeySpec(DEV_KEY, "AES"));
		}
		for (String entry : oldKeys.split(",")) {
			if (entry.isBlank()) {
				continue;
			}
			int eq = entry.indexOf('=');
			if (eq <= 0) {
				throw new IllegalStateException("MASAPAY_SECRETS_OLD_KEYS must look like 'k0=<base64>,k1=<base64>'");
			}
			all.put(entry.substring(0, eq).trim(), key("MASAPAY_SECRETS_OLD_KEYS", entry.substring(eq + 1).trim()));
		}
		if (encryptionKey == null || encryptionKey.isBlank()) {
			if (!allowDevSecrets) {
				throw new IllegalStateException("MASAPAY_SECRETS_ENCRYPTION_KEY is not set. Set a random 32-byte key"
					+ " (e.g. `openssl rand -base64 32`), or MASAPAY_ALLOW_DEV_SECRETS=true for local development.");
			}
			this.currentKeyId = DEV_KEY_ID;
		}
		else {
			if (keyId.isBlank() || keyId.contains(":")) {
				throw new IllegalStateException("MASAPAY_SECRETS_KEY_ID must be a short name without ':'");
			}
			all.put(keyId, key("MASAPAY_SECRETS_ENCRYPTION_KEY", encryptionKey.trim()));
			this.currentKeyId = keyId;
		}
		this.keys = Map.copyOf(all);
	}

	/** For tests: the given keys, encrypting with {@code currentKeyId}. */
	SecretCipher(Map<String, SecretKey> keys, String currentKeyId) {
		this.keys = Map.copyOf(keys);
		this.currentKeyId = currentKeyId;
	}

	static SecretKey key(String name, String base64) {
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(base64);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException(name + " is not valid base64");
		}
		if (bytes.length != KEY_BYTES) {
			throw new IllegalStateException(name + " must be " + KEY_BYTES + " bytes (base64 of 32 random bytes)");
		}
		return new SecretKeySpec(bytes, "AES");
	}

	/** {@code enc:<current key id>:...}, or null for null. */
	public String encrypt(String plain) {
		if (plain == null) {
			return null;
		}
		byte[] iv = new byte[IV_BYTES];
		random.nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, keys.get(currentKeyId), new GCMParameterSpec(TAG_BITS, iv));
			cipher.updateAAD(currentKeyId.getBytes(StandardCharsets.UTF_8));
			byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] out = ByteBuffer.allocate(iv.length + sealed.length).put(iv).put(sealed).array();
			return PREFIX + currentKeyId + ":" + Base64.getEncoder().encodeToString(out);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("Could not encrypt a secret", e);
		}
	}

	/**
	 * The clear text of a stored value; clear text from before encryption comes back unchanged.
	 *
	 * @throws IllegalStateException when the key id is unknown or the value does not decrypt
	 *     (wrong key, or the value was altered)
	 */
	public String decrypt(String stored) {
		if (stored == null || !stored.startsWith(PREFIX)) {
			return stored;
		}
		int colon = stored.indexOf(':', PREFIX.length());
		if (colon < 0) {
			throw new IllegalStateException("Malformed encrypted secret");
		}
		String keyId = stored.substring(PREFIX.length(), colon);
		SecretKey key = keys.get(keyId);
		if (key == null) {
			throw new IllegalStateException("No key '" + keyId + "' to decrypt a secret; is MASAPAY_SECRETS_OLD_KEYS"
				+ " missing it?");
		}
		try {
			byte[] in = Base64.getDecoder().decode(stored.substring(colon + 1));
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
			cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
			byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
			return new String(plain, StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new IllegalStateException("A secret does not decrypt with key '" + keyId + "' (wrong key?)", e);
		}
	}

	/** True for clear text and for values under an older key: they should be written again. */
	public boolean needsReEncryption(String stored) {
		return stored != null && !stored.startsWith(PREFIX + currentKeyId + ":");
	}
}
