package az.masapay.config;

import az.masapay.security.JwtAuthenticationFilter;
import az.masapay.security.JwtService;
import az.masapay.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security. A Bearer access token is validated on every request by
 * {@link JwtAuthenticationFilter}; unauthenticated access to protected endpoints
 * returns 401 via {@link RestAuthenticationEntryPoint}. The public surface is the
 * actuator health/info probes and the authentication endpoints. Full role-based
 * authorization (@PreAuthorize) is added in EPIC 3 (#16).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http, JwtService jwtService, ObjectMapper objectMapper) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// Allow the container error dispatch so controller errors keep their status.
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers("/api/auth/**").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper)))
			.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable);

		return http.build();
	}
}
