package az.masapay.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecretCipherTest {

	private static final String KEY_1 = Base64.getEncoder().encodeToString(new byte[32]);
	private static final String KEY_2 = Base64.getEncoder().encodeToString("another-thirty-two-byte-key-1234".getBytes());

	private final SecretCipher cipher = new SecretCipher(KEY_1, "k1", "", false);

	@Test
	void encryptsAndDecryptsWithTheKeyIdInFront() {
		String stored = cipher.encrypt("payriff-secret");

		assertThat(stored).startsWith("enc:k1:").doesNotContain("payriff-secret");
		assertThat(cipher.decrypt(stored)).isEqualTo("payriff-secret");
		assertThat(cipher.encrypt(null)).isNull();
		assertThat(cipher.decrypt(null)).isNull();
	}

	@Test
	void everyValueGetsItsOwnIv() {
		assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
	}

	@Test
	void theWrongKeyOrAnAlteredValueDoesNotDecrypt() {
		String stored = cipher.encrypt("payriff-secret");
		SecretCipher other = new SecretCipher(KEY_2, "k1", "", false);

		assertThatThrownBy(() -> other.decrypt(stored)).isInstanceOf(IllegalStateException.class);
		String altered = stored.substring(0, stored.length() - 4) + (stored.endsWith("AAAA") ? "BBBB" : "AAAA");
		assertThatThrownBy(() -> cipher.decrypt(altered)).isInstanceOf(IllegalStateException.class);
		// A value can not be passed off as another key's: the key id is authenticated too.
		assertThatThrownBy(() -> cipher.decrypt(stored.replace("enc:k1:", "enc:k2:")))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void clearTextFromBeforeEncryptionIsReadAndMarkedForEncryption() {
		assertThat(cipher.decrypt("old-plain-key")).isEqualTo("old-plain-key");
		assertThat(cipher.needsReEncryption("old-plain-key")).isTrue();
		assertThat(cipher.needsReEncryption(cipher.encrypt("x"))).isFalse();
		assertThat(cipher.needsReEncryption(null)).isFalse();
	}

	@Test
	void anOldKeyStillDecryptsAfterRotation() {
		String underOldKey = cipher.encrypt("payriff-secret");
		SecretCipher rotated = new SecretCipher(KEY_2, "k2", "k1=" + KEY_1, false);

		assertThat(rotated.decrypt(underOldKey)).isEqualTo("payriff-secret");
		assertThat(rotated.needsReEncryption(underOldKey)).isTrue();
		assertThat(rotated.encrypt("payriff-secret")).startsWith("enc:k2:");
	}

	@Test
	void refusesToStartWithoutAValidKeyOutsideDevelopment() {
		assertThatThrownBy(() -> new SecretCipher("", "k1", "", false))
			.hasMessageContaining("MASAPAY_SECRETS_ENCRYPTION_KEY is not set");
		assertThatThrownBy(() -> new SecretCipher("c2hvcnQ=", "k1", "", false)).hasMessageContaining("32 bytes");
		assertThatThrownBy(() -> new SecretCipher("not base64!", "k1", "", false)).hasMessageContaining("base64");

		SecretCipher dev = new SecretCipher("", "k1", "", true);
		assertThat(dev.encrypt("x")).startsWith("enc:dev:");
	}

	@Test
	void theTestConstructorUsesTheGivenKeys() {
		SecretCipher fromMap = new SecretCipher(Map.of("t", SecretCipher.key("test", KEY_1)), "t");
		assertThat(fromMap.decrypt(fromMap.encrypt("v"))).isEqualTo("v");
	}
}
