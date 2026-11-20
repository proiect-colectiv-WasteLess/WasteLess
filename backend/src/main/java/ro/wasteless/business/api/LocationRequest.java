package ro.wasteless.business.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record LocationRequest(
    @Size(max = 80) String label,
    @NotBlank @Size(max = 255) String addressLine1,
    @Size(max = 255) String addressLine2,
    @NotBlank @Size(max = 120) String city,
    @Size(max = 20) String postalCode,
    @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
    @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 6)
        BigDecimal latitude,
    @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 6)
        BigDecimal longitude,
    Boolean isPrimary) {}
