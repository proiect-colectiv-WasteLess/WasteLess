package ro.wasteless.services;
import ro.wasteless.dto.Api;
import ro.wasteless.entities.*;
import ro.wasteless.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final long ttl;
    private final String dummyHash;
    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder,
        @Value("${app.jwt.ttl-seconds:3600}") long ttl) {
        if (ttl < 1) throw new IllegalArgumentException("JWT TTL must be positive");
        this.users=users; this.passwords=passwords; this.encoder=encoder; this.ttl=ttl;
        dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    @Transactional
    public Api.Auth register(Api.Register r) {
        if (r.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400,"Password must contain at most 72 UTF-8 bytes");
        if (r.role()==Role.BUSINESS && (r.businessName()==null || r.businessName().isBlank()
            || r.pickupLocation()==null || r.pickupLocation().isBlank()))
            throw new ApiException(400,"Business name and pickup location are required");
        String email=normalize(r.email());
        if (users.findByEmail(email).isPresent()) throw new ApiException(409,"Email already registered");
        UserAccount u=new UserAccount(); u.email=email; u.passwordHash=passwords.encode(r.password()); u.role=r.role();
        if(u.role==Role.BUSINESS) { u.businessName=r.businessName().strip(); u.pickupLocation=r.pickupLocation().strip(); }
        users.saveAndFlush(u);
        return token(u);
    }
    public Api.Auth login(Api.Login r) {
        UserAccount u=users.findByEmail(normalize(r.email())).orElse(null);
        boolean valid=r.password().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwords.matches(r.password(),u==null ? dummyHash : u.passwordHash);
        if(u==null || !valid) throw new ApiException(401,"Invalid email or password");
        return token(u);
    }
    private Api.Auth token(UserAccount u) {
        Instant now=Instant.now(), end=now.plusSeconds(ttl);
        var claims=JwtClaimsSet.builder().issuer("wasteless").subject(u.id.toString())
            .issuedAt(now).expiresAt(end).claim("role",u.role.name()).build();
        String jwt=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Api.Auth(jwt,"Bearer",end,Api.User.of(u));
    }
}