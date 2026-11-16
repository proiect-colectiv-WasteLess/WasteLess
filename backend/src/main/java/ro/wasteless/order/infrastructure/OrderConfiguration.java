package ro.wasteless.order.infrastructure;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import ro.wasteless.common.port.OfferStockPort;

@Configuration
@org.springframework.scheduling.annotation.EnableScheduling
@EnableMethodSecurity
public class OrderConfiguration {
  @Bean
  @ConditionalOnMissingBean(OfferStockPort.class)
  OfferStockPort catalogStock(JdbcTemplate jdbc, Clock clock) {
    return new CatalogStockAdapter(jdbc, clock);
  }
}
