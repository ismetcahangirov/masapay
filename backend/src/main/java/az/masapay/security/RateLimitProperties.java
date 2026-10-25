package az.masapay.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Request limits per client IP (anonymous) or per user (staff); customer limits are per table and
 * IP, with {@code publicPerMinutePerIp} over all tables of one IP. See application.yml.
 */
@ConfigurationProperties(prefix = "masapay.rate-limit")
public record RateLimitProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("20") int authPerMinute,
		@DefaultValue("120") int publicPerMinute,
		@DefaultValue("10") int paymentPerMinute,
		@DefaultValue("100") int paymentPerDay,
		@DefaultValue("300") int staffPerMinute,
		@DefaultValue("3000") int publicPerMinutePerIp) {
}
