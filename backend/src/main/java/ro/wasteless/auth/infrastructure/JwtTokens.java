package ro.wasteless.auth.infrastructure;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JwtTokens {
  public static final String ISSUER = "wasteless";
  private final JwtEncoder encoder;
  private final Clock clock;

  public JwtTokens(@Value("${app.jwt-secret}") String secret, Clock clock) {
    byte[] key = secret.getBytes(StandardCharsets.UTF_8);
    if (key.length < 32)
      throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    this.clock = clock;
  }

  public String issue(UUID user, List<String> roles) {
    var now = clock.instant();
    var claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(user.toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .claim("roles", roles)
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  @Configuration
  public static class DecoderConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.jwt-secret}") String secret, Clock clock) {
      var decoder =
          NimbusJwtDecoder.withSecretKey(
                  new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
              .macAlgorithm(MacAlgorithm.HS256)
              .build();
      var time = new JwtTimestampValidator(Duration.ZERO);
      time.setClock(clock);
      OAuth2TokenValidator<Jwt> required =
          jwt -> {
            try {
              UUID.fromString(jwt.getSubject());
              if (jwt.getExpiresAt() == null
                  || jwt.getIssuedAt() == null
                  || jwt.getIssuedAt().isAfter(clock.instant())
                  || !jwt.getExpiresAt().isAfter(clock.instant())
                  || jwt.getId() == null
                  || jwt.getClaimAsStringList("roles") == null)
                throw new IllegalArgumentException();
              return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException e) {
              return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
            }
          };
      decoder.setJwtValidator(
          new DelegatingOAuth2TokenValidator<>(time, new JwtIssuerValidator(ISSUER), required));
      return decoder;
    }
  }
}
