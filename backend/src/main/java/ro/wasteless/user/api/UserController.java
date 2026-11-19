package ro.wasteless.user.api;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.auth.api.*;
import ro.wasteless.auth.api.AccountRequests.*;
import ro.wasteless.auth.api.UserAccountOperations;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
  private final UserAccountOperations accounts;

  public UserController(UserAccountOperations accounts) {
    this.accounts = accounts;
  }

  @GetMapping
  public UserView me() {
    return accounts.me();
  }

  @PatchMapping
  public UserView patch(@RequestBody Map<String, Object> body) {
    return accounts.patch(body);
  }

  @PostMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(@Valid @RequestBody ChangePassword body) {
    accounts.changePassword(body.currentPassword(), body.newPassword());
  }

  @PutMapping("/location")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void location(@Valid @RequestBody Location body) {
    accounts.putLocation(body);
  }

  @DeleteMapping("/location")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeLocation() {
    accounts.deleteLocation();
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> delete(@Valid @RequestBody DeleteAccount body) {
    accounts.deleteAccount(body.password());
    return Map.of("message", "Account deletion accepted.");
  }
}
