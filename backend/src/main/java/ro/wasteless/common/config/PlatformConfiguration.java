package ro.wasteless.common.config;

import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingTime")
public class PlatformConfiguration {
  @Bean
  org.springframework.data.auditing.DateTimeProvider auditingTime(Clock clock) {
    return () -> java.util.Optional.of(clock.instant());
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
