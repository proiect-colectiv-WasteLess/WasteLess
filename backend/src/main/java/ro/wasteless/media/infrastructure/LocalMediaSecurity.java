package ro.wasteless.media.infrastructure;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"dev", "test"})
public class LocalMediaSecurity {
  @Bean
  @Order(0)
  SecurityFilterChain localMedia(HttpSecurity http) throws Exception {
    return http.securityMatcher("/media/images/**")
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.GET, "/media/images/*")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .build();
  }
}
