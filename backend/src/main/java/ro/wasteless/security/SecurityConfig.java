package ro.wasteless.security;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.web.cors.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(); }
    @Bean SecretKeySpec jwtKey(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes=secret.getBytes(StandardCharsets.UTF_8);
        if(bytes.length<32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        return new SecretKeySpec(bytes,"HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKeySpec key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKeySpec key) {
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> identity=jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                String role=jwt.getClaimAsString("role");
                if(jwt.getExpiresAt()!=null && ("CLIENT".equals(role)||"BUSINESS".equals(role))) return OAuth2TokenValidatorResult.success();
            } catch(RuntimeException ignored) {}
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
            new JwtIssuerValidator("wasteless"),identity));
        return decoder;
    }
    @Bean CorsConfigurationSource cors(@Value("${app.cors.allowed-origins:}") String origins) {
        CorsConfiguration config=new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::strip).filter(s->!s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET","POST","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type"));
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**",config); return source;
    }
    private void error(HttpServletResponse response,int status,String message) throws java.io.IOException {
        response.setStatus(status); response.setContentType("application/json");
        response.getWriter().write("{\"timestamp\":\""+java.time.Instant.now()+"\",\"status\":"+status+",\"message\":\""+message+"\",\"details\":[]}");
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter authorities=new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role"); authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter=new JwtAuthenticationConverter(); converter.setJwtGrantedAuthoritiesConverter(authorities);
        return http.csrf(csrf->csrf.disable()).cors(cors->{})
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a
                .requestMatchers(HttpMethod.POST,"/api/v1/auth/register","/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/business/**").hasRole("BUSINESS")
                .requestMatchers(HttpMethod.POST,"/api/v1/offers").hasRole("BUSINESS")
                .requestMatchers(HttpMethod.GET,"/api/v1/offers").hasRole("CLIENT")
                .requestMatchers(HttpMethod.POST,"/api/v1/reservations").hasRole("CLIENT")
                .requestMatchers("/api/v1/reservations/me").hasRole("CLIENT")
                .requestMatchers("/api/v1/offers/*","/api/v1/reservations/*").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->error(r,401,"Authentication required"))
                .accessDeniedHandler((q,r,x)->error(r,403,"Access denied")))
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((q,r,x)->error(r,401,"Invalid or expired token"))
                .accessDeniedHandler((q,r,x)->error(r,403,"Access denied")))
            .build();
    }
}