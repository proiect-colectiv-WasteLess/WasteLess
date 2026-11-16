package ro.wasteless.common.config;

import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class PlatformConfiguration {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
