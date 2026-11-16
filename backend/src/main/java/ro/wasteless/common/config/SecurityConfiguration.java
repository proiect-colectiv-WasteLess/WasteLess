package ro.wasteless.common.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import ro.wasteless.common.api.*;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfiguration {
  @Bean
  CorsConfigurationSource cors(@Value("${app.cors-origins}") String origins) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    c.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(
        java.util.List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Request-Id"));
    c.setExposedHeaders(java.util.List.of("X-Request-Id"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, Environment env, JsonMapper mapper)
      throws Exception {
    http.cors(c -> {})
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(
        a -> {
          a.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
          a.requestMatchers(
                  HttpMethod.GET,
                  "/api/v1/categories",
                  "/api/v1/offers/nearby",
                  "/api/v1/offers/*",
                  "/api/v1/businesses/*")
              .permitAll();
          if (env.acceptsProfiles(Profiles.of("dev")))
            a.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
          a.anyRequest().authenticated();
        });
    http.exceptionHandling(
        e ->
            e.authenticationEntryPoint(
                    (req, res, ex) -> {
                      res.setStatus(401);
                      res.setContentType("application/problem+json");
                      res.getWriter()
                          .write(
                              mapper.writeValueAsString(
                                  ProblemResponses.of(
                                      HttpStatus.UNAUTHORIZED,
                                      ErrorCode.UNAUTHORIZED,
                                      "Authentication required")));
                    })
                .accessDeniedHandler(
                    (req, res, ex) -> {
                      res.setStatus(403);
                      res.setContentType("application/problem+json");
                      res.getWriter()
                          .write(
                              mapper.writeValueAsString(
                                  ProblemResponses.of(
                                      HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Access denied")));
                    }));
    return http.build();
  }
}
