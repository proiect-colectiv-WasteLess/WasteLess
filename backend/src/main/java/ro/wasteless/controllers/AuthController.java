package ro.wasteless.controllers;
import ro.wasteless.dto.Api;
import ro.wasteless.services.AuthService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth=auth; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public Api.Auth register(@Valid @RequestBody Api.Register request) { return auth.register(request); }
    @PostMapping("/login")
    public Api.Auth login(@Valid @RequestBody Api.Login request) { return auth.login(request); }
}