package az.masapay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs, such as the Payriff reconciliation. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
