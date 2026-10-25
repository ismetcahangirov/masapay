package az.masapay.config;

import az.masapay.security.FixedWindowRateLimiter;
import az.masapay.security.JwtAuthenticationFilter;
import az.masapay.security.JwtService;
import az.masapay.security.RateLimitFilter;
import az.masapay.security.RateLimitProperties;
import az.masapay.security.RateLimiter;
import az.masapay.security.RedisRateLimiter;
import az.masapay.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
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
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			ObjectMapper objectMapper, RateLimitProperties rateLimits,
			ObjectProvider<StringRedisTemplate> redis,
			@Value("${masapay.redis.enabled:true}") boolean redisEnabled) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				// Allow the container error dispatch so controller errors keep their status.
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers("/api/auth/**").permitAll()
				.requestMatchers("/api/public/**").permitAll()
				.requestMatchers("/ws/**").permitAll()
				// Swagger UI and the OpenAPI spec; turned off entirely by MASAPAY_API_DOCS_ENABLED=false.
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(ex -> ex.authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper)))
			.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable);

		// After the JWT filter, so staff are limited per user rather than per shared office IP.
		// Counters live in Redis when it is on, shared by every instance (#158); in memory otherwise.
		if (rateLimits.enabled()) {
			StringRedisTemplate template = redisEnabled ? redis.getIfAvailable() : null;
			RateLimiter limiter = template != null
				? new RedisRateLimiter(template, Clock.systemUTC())
				: new FixedWindowRateLimiter(Clock.systemUTC());
			http.addFilterAfter(new RateLimitFilter(rateLimits, limiter, objectMapper), JwtAuthenticationFilter.class);
		}

		return http.build();
	}
}
