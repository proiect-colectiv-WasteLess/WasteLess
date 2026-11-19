package ro.wasteless.auth.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AccountRequests {
  private AccountRequests() {}

  public static final String PASSWORD = "(?s)(?=.*[A-Za-z])(?=.*[0-9]).{10,72}";
  public static final String PHONE = "\\+?[0-9 ()-]{7,30}";

  public record Register(
      @NotNull Role role,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Pattern(regexp = PASSWORD) String password,
      @NotBlank @Size(max = 80) String firstName,
      @NotBlank @Size(max = 80) String lastName,
      @Pattern(regexp = PHONE) String phone,
      @Size(max = 120) String businessName,
      @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
      @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
    @AssertTrue(message = "Business name is required for BUSINESS registration")
    public boolean isBusinessValid() {
      return role != Role.BUSINESS || (businessName != null && !businessName.isBlank());
    }

    @AssertTrue(message = "Coordinates must be supplied together")
    public boolean isCoordinatesValid() {
      return (latitude == null) == (longitude == null);
    }

    @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
    public boolean isPasswordValid() {
      return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
      return "Register[redacted]";
    }
  }

  public enum Role {
    CLIENT,
    BUSINESS
  }

  public record Login(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 72) String password,
      @Size(max = 255) String deviceId) {
    @Override
    public String toString() {
      return "Login[redacted]";
    }
  }

  public record Token(@NotBlank @Size(max = 128) String refreshToken) {
    @Override
    public String toString() {
      return "Token[redacted]";
    }
  }

  public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}

  public record ConfirmPassword(
      @NotBlank @Size(max = 128) String token,
      @NotBlank @Pattern(regexp = PASSWORD) String newPassword) {
    @Override
    public String toString() {
      return "ConfirmPassword[redacted]";
    }
  }

  public record ConfirmToken(@NotBlank @Size(max = 128) String token) {
    @Override
    public String toString() {
      return "ConfirmToken[redacted]";
    }
  }

  public record ChangePassword(
      @NotBlank @Size(max = 72) String currentPassword,
      @NotBlank @Pattern(regexp = PASSWORD) String newPassword) {
    @Override
    public String toString() {
      return "ChangePassword[redacted]";
    }
  }

  public record DeleteAccount(@NotBlank @Size(max = 72) String password) {
    @Override
    public String toString() {
      return "DeleteAccount[redacted]";
    }
  }

  public record Location(
      @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
      @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
      @DecimalMin("0") @DecimalMax("99999.99") BigDecimal accuracyMeters) {}

  public record RoleChange(
      @NotNull Set<@NotBlank @Pattern(regexp = "ROLE_(CLIENT|BUSINESS|ADMIN)") String> add,
      @NotNull Set<@NotBlank @Pattern(regexp = "ROLE_(CLIENT|BUSINESS|ADMIN)") String> remove) {
    @AssertTrue(message = "A role cannot be both added and removed")
    public boolean isDisjoint() {
      return add == null || remove == null || Collections.disjoint(add, remove);
    }
  }
}
