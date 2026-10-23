package az.masapay.security;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once at every startup (#157). Encrypts restaurant secrets still stored in clear text (rows
 * from before encryption) and re-encrypts values under an older key after a rotation. It also
 * proves the configured key opens every stored value: with a wrong or missing key the application
 * stops here, instead of failing later on a customer's payment.
 * <p>
 * Plain JDBC on purpose: through the entity, Hibernate would see an unchanged clear-text attribute
 * and write nothing back. Without a database (smoke tests) it does nothing.
 */
@Component
public class RestaurantSecretsEncryptor implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(RestaurantSecretsEncryptor.class);

	/** The encrypted columns of {@code restaurants}; fixed names, never user input. */
	static final List<String> COLUMNS = List.of("payriff_secret_key", "iiko_api_login");

	private final ObjectProvider<JdbcTemplate> jdbcProvider;
	private final SecretCipher cipher;

	public RestaurantSecretsEncryptor(ObjectProvider<JdbcTemplate> jdbcProvider, SecretCipher cipher) {
		this.jdbcProvider = jdbcProvider;
		this.cipher = cipher;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		JdbcTemplate jdbc = jdbcProvider.getIfAvailable();
		if (jdbc == null) {
			return;
		}
		int rewritten = 0;
		for (String column : COLUMNS) {
			List<Row> rows = jdbc.query("SELECT id, " + column + " FROM restaurants WHERE " + column + " IS NOT NULL",
				(rs, n) -> new Row(rs.getObject(1, UUID.class), rs.getString(2)));
			for (Row row : rows) {
				String plain = cipher.decrypt(row.value());
				if (cipher.needsReEncryption(row.value())) {
					jdbc.update("UPDATE restaurants SET " + column + " = ? WHERE id = ?", cipher.encrypt(plain), row.id());
					rewritten++;
				}
			}
		}
		if (rewritten > 0) {
			log.info("Encrypted {} restaurant secret(s) with the current key", rewritten);
		}
	}

	private record Row(UUID id, String value) {
	}
}
