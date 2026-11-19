package ro.wasteless.auth.infrastructure;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import ro.wasteless.common.api.*;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  AuthenticationManager authenticationManager(
      ro.wasteless.auth.application.AccountService accounts) {
    return new org.springframework.security.authentication.ProviderManager(
        new org.springframework.security.authentication.AuthenticationProvider() {
          @Override
          public boolean supports(Class<?> type) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
          }

          @Override
          public org.springframework.security.core.Authentication authenticate(
              org.springframework.security.core.Authentication authentication) {
            try {
              var user =
                  accounts.authenticate(
                      authentication.getName(), String.valueOf(authentication.getCredentials()));
              return UsernamePasswordAuthenticationToken.authenticated(
                  user.id().toString(),
                  null,
                  user.roles().stream()
                      .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                      .toList());
            } catch (DomainException e) {
              throw new org.springframework.security.authentication.BadCredentialsException(
                  "Invalid credentials");
            }
          }
        });
  }

  @Bean
  CorsConfigurationSource cors(@Value("${app.cors-origins}") String origins) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    if (c.getAllowedOrigins().contains("*"))
      throw new IllegalArgumentException("CORS_ORIGINS requires explicit origins");
    c.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(
        java.util.List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Request-Id"));
    c.setExposedHeaders(java.util.List.of("X-Request-Id", "Retry-After"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      Environment env,
      JsonMapper mapper,
      ro.wasteless.auth.infrastructure.UserRepository users,
      java.time.Clock clock,
      AuthRateLimiter limiter)
      throws Exception {
    http.cors(c -> {})
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(
        a -> {
          a.requestMatchers(
                  HttpMethod.POST,
                  "/api/v1/auth/register",
                  "/api/v1/auth/login",
                  "/api/v1/auth/refresh",
                  "/api/v1/auth/logout",
                  "/api/v1/auth/password-reset/request",
                  "/api/v1/auth/password-reset/confirm",
                  "/api/v1/auth/email-verification/request",
                  "/api/v1/auth/email-verification/confirm")
              .permitAll();
          a.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
          a.requestMatchers(
                  HttpMethod.GET,
                  "/api/v1/categories",
                  "/api/v1/offers/nearby",
                  "/api/v1/offers/{id}")
              .permitAll();
          // Explicitly match /me before the public business UUID detail route.
          a.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
          a.requestMatchers("/api/v1/businesses/me", "/api/v1/businesses/me/**")
              .hasRole("BUSINESS");
          a.requestMatchers(HttpMethod.GET, "/api/v1/businesses/{id}").permitAll();
          a.requestMatchers("/api/v1/orders", "/api/v1/orders/**", "/api/v1/users/me/impact")
              .hasRole("CLIENT");
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
    var authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("");
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    http.oauth2ResourceServer(
        o ->
            o.jwt(j -> j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint(
                    (req, res, ex) ->
                        SecurityProblems.write(res, mapper, 401, "Authentication required"))
                .accessDeniedHandler(
                    (req, res, ex) -> SecurityProblems.write(res, mapper, 403, "Access denied")));
    http.addFilterBefore(
        new AuthRateFilter(limiter, mapper), BearerTokenAuthenticationFilter.class);
    http.addFilterAfter(
        new AccountStatusFilter(users, clock, mapper), BearerTokenAuthenticationFilter.class);
    return http.build();
  }
}
