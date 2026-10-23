package az.masapay.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Keeps a String column encrypted in the database and clear in the entity (#157). Hibernate gets
 * this converter from Spring, so the {@link SecretCipher} with the configured key is injected.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

	private final SecretCipher cipher;

	public EncryptedStringConverter(SecretCipher cipher) {
		this.cipher = cipher;
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return cipher.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return cipher.decrypt(dbData);
	}
}
