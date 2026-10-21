package az.masapay.admin;

/**
 * Masks secrets for display. Only the last four characters are kept, behind a
 * fixed-width mask so the real length is never revealed.
 */
public final class SecretMasker {

	/** Fixed mask prefix: eight bullet characters, independent of the secret length. */
	public static final String MASK = "•".repeat(8);

	private static final int VISIBLE_CHARS = 4;

	private SecretMasker() {
	}

	/** Returns the masked form of a secret, or null when there is no secret. */
	public static String mask(String secret) {
		if (secret == null || secret.isEmpty()) {
			return null;
		}
		if (secret.length() <= VISIBLE_CHARS) {
			return MASK;
		}
		return MASK + secret.substring(secret.length() - VISIBLE_CHARS);
	}

	/**
	 * Whether an incoming value means "keep the stored secret": null, blank, or
	 * the masked placeholder the client was given for the current secret.
	 */
	public static boolean isUnchanged(String incoming, String current) {
		if (incoming == null || incoming.isBlank()) {
			return true;
		}
		return incoming.equals(mask(current));
	}
}
