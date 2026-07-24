package az.masapay.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal baseline security configuration for the project skeleton.
 * <p>
 * Only the public actuator health/info probes are exposed anonymously; every
 * other endpoint requires authentication. Full authentication and RBAC
 * (Google OAuth2, JWT, roles) are implemented in EPIC 3.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// Allow the container error dispatch so controller errors keep their status.
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers("/api/auth/**").permitAll()
				.anyRequest().authenticated())
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable);

		return http.build();
	}
}
