package ro.wasteless.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.auth.api.AccountRequests.*;
import ro.wasteless.auth.application.AccountService;
import ro.wasteless.auth.infrastructure.AuthRateLimiter;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AccountService accounts;
  private final AuthRateLimiter limiter;

  public AuthController(AccountService accounts, AuthRateLimiter limiter) {
    this.accounts = accounts;
    this.limiter = limiter;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserView register(@Valid @RequestBody Register body) {
    limiter.account("register", body.email());
    return accounts.register(body);
  }

  @PostMapping("/login")
  public SessionTokens login(@Valid @RequestBody Login body, HttpServletRequest request) {
    limiter.account("login", body.email());
    return accounts.login(body, request.getHeader("User-Agent"), request.getRemoteAddr());
  }

  @PostMapping("/refresh")
  public SessionTokens refresh(@Valid @RequestBody Token body, HttpServletRequest request) {
    return accounts.rotate(
        body.refreshToken(), request.getHeader("User-Agent"), request.getRemoteAddr());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody Token body) {
    accounts.logout(body.refreshToken());
  }

  @PostMapping("/logout-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logoutAll() {
    accounts.logoutAll();
  }

  @GetMapping("/sessions")
  public List<AccountService.SessionView> sessions() {
    return accounts.sessions();
  }

  @DeleteMapping("/sessions/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@PathVariable UUID id) {
    accounts.revokeSession(id);
  }

  @PostMapping("/password-reset/request")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> requestReset(@Valid @RequestBody EmailRequest body) {
    limiter.account("password-reset", body.email());
    accounts.requestReset(body.email());
    return generic();
  }

  @PostMapping("/password-reset/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void confirmReset(@Valid @RequestBody ConfirmPassword body) {
    accounts.confirmReset(body.token(), body.newPassword());
  }

  @PostMapping("/email-verification/request")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> requestVerification(@Valid @RequestBody EmailRequest body) {
    limiter.account("email-verification", body.email());
    accounts.requestVerification(body.email());
    return generic();
  }

  @PostMapping("/email-verification/confirm")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void confirmVerification(@Valid @RequestBody ConfirmToken body) {
    accounts.confirmVerification(body.token());
  }

  private Map<String, String> generic() {
    return Map.of("message", "If the account is eligible, an email will be sent.");
  }
}
