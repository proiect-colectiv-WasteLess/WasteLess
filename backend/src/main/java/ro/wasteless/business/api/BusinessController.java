package ro.wasteless.business.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.business.domain.Business;
import ro.wasteless.business.infrastructure.BusinessRepository;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {
  private final BusinessRepository businesses;
  private final BusinessAccess access;

  public BusinessController(BusinessRepository businesses, BusinessAccess access) {
    this.businesses = businesses;
    this.access = access;
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('BUSINESS')")
  @Transactional(readOnly = true)
  public PrivateProfile me() {
    return privateView(access.current());
  }

  @GetMapping("/{id}")
  @Transactional(readOnly = true)
  public PublicProfile publicProfile(@PathVariable UUID id) {
    var b =
        businesses
            .findById(id)
            .filter(x -> x.deletedAt == null && "ACTIVE".equals(x.status))
            .orElseThrow(BusinessAccess::notFound);
    return new PublicProfile(
        b.getId(),
        b.name,
        b.slug,
        b.description,
        b.logoUrl,
        b.coverUrl,
        b.ratingAvg,
        b.ratingCount,
        b.isVerified);
  }

  @PatchMapping("/me")
  @PreAuthorize("hasRole('BUSINESS')")
  @Transactional
  public PrivateProfile update(@Valid @RequestBody ProfilePatch p) {
    var b = access.lockCurrent();
    if (p.name() != null) {
      if (p.name().isBlank()) throw new CatalogValidationException("name", "Must not be blank");
      b.name = p.name().trim();
    }
    if (p.description() != null) b.description = p.description().trim();
    if (p.phone() != null) b.phone = p.phone().trim();
    if (p.email() != null) b.email = p.email().trim();
    if (p.logoUrl() != null) b.logoUrl = p.logoUrl();
    if (p.coverUrl() != null) b.coverUrl = p.coverUrl();
    if (p.vatNumber() != null) b.vatNumber = p.vatNumber().trim();
    return privateView(b);
  }

  private PrivateProfile privateView(Business b) {
    return new PrivateProfile(
        b.getId(),
        b.name,
        b.slug,
        b.description,
        b.phone,
        b.email,
        b.logoUrl,
        b.coverUrl,
        b.vatNumber,
        b.ratingAvg,
        b.ratingCount,
        b.isVerified,
        b.status);
  }

  public record ProfilePatch(
      @Size(min = 1, max = 120) String name,
      @Size(max = 2000) String description,
      @Size(max = 30) String phone,
      @Email @Size(max = 254) String email,
      @Size(max = 2048) String logoUrl,
      @Size(max = 2048) String coverUrl,
      @Size(max = 40) String vatNumber) {}

  public record PrivateProfile(
      UUID id,
      String name,
      String slug,
      String description,
      String phone,
      String email,
      String logoUrl,
      String coverUrl,
      String vatNumber,
      BigDecimal ratingAvg,
      int ratingCount,
      boolean isVerified,
      String status) {}

  public record PublicProfile(
      UUID id,
      String name,
      String slug,
      String description,
      String logoUrl,
      String coverUrl,
      BigDecimal ratingAvg,
      int ratingCount,
      boolean isVerified) {}
}
