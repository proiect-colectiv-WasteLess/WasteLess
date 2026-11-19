package ro.wasteless.support;

import java.time.*;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@TestConfiguration
public class TestFoundation {
  public static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

  @Bean
  @Primary
  Clock testClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  public static AutoCloseable asUser(UUID user, String role) {
    var previous = SecurityContextHolder.getContext();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            user.toString(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    SecurityContextHolder.setContext(context);
    return () -> SecurityContextHolder.setContext(previous);
  }
}
