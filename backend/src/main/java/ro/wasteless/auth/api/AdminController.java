package ro.wasteless.auth.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.auth.application.AccountService;
import ro.wasteless.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminController {
  private final AccountService accounts;

  public AdminController(AccountService accounts) {
    this.accounts = accounts;
  }

  @GetMapping
  public PageResponse<UserView> list(
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return accounts.listUsers(email, status, role, page, size);
  }

  @PostMapping("/{id}/lock")
  public UserView lock(@PathVariable UUID id) {
    return accounts.adminStatus(id, "lock");
  }

  @PostMapping("/{id}/unlock")
  public UserView unlock(@PathVariable UUID id) {
    return accounts.adminStatus(id, "unlock");
  }

  @PostMapping("/{id}/suspend")
  public UserView suspend(@PathVariable UUID id) {
    return accounts.adminStatus(id, "suspend");
  }

  @PostMapping("/{id}/reactivate")
  public UserView reactivate(@PathVariable UUID id) {
    return accounts.adminStatus(id, "reactivate");
  }

  @PostMapping("/{id}/roles")
  public UserView roles(
      @PathVariable UUID id, @Valid @RequestBody AccountRequests.RoleChange body) {
    return accounts.adminRoles(id, body);
  }
}
