package az.masapay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Skeleton smoke test: verifies the application context boots and the public
 * actuator health endpoint responds without requiring external infrastructure.
 * <p>
 * The database, Flyway and Redis auto-configurations are excluded here because
 * they are validated against real services (Testcontainers) in issue #52. This
 * keeps {@code ./gradlew build} green on any machine and in CI (issue #51).
 */
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = "spring.autoconfigure.exclude="
		+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration")
class MasapayApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void contextLoads() {
		assertThat(restTemplate).isNotNull();
	}

	@Test
	void healthEndpointReportsUp() {
		ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"status\":\"UP\"");
	}
}
