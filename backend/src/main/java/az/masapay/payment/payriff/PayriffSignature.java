package az.masapay.payment.payriff;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * HMAC-SHA256 signatures for Payriff callbacks (#34). Payriff does not sign its callbacks, so we
 * sign the data we put into the callback URL ourselves and check it when the webhook comes back.
 */
@Component
public class PayriffSignature {

	private static final String ALGORITHM = "HmacSHA256";

	private final SecretKeySpec key;

	public PayriffSignature(@Value("${masapay.payriff.webhook-secret}") String secret) {
		this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
	}

	/** Hex HMAC of the data; the same data and key always give the same signature. */
	public String sign(String data) {
		try {
			// Mac is not thread-safe, so every call gets its own instance.
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(key);
			return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("HmacSHA256 is not available", e);
		}
	}

	/** Whether the signature belongs to the data; compares in constant time. */
	public boolean matches(String data, String signature) {
		if (data == null || signature == null) {
			return false;
		}
		return MessageDigest.isEqual(
				sign(data).getBytes(StandardCharsets.UTF_8),
				signature.getBytes(StandardCharsets.UTF_8));
	}

	/** Rejects a webhook whose signature does not match its data with 403 Forbidden. */
	public void verify(String data, String signature) {
		if (!matches(data, signature)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Payriff signature");
		}
	}
}
