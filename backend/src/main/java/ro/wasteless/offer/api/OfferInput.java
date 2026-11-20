package ro.wasteless.offer.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OfferInput(
    @NotBlank @Size(min = 3, max = 120) String title,
    @NotBlank String category,
    @Size(max = 2000) String description,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2)
        BigDecimal originalPrice,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2)
        BigDecimal price,
    @NotNull @Min(1) @Max(10000) Integer quantity,
    @NotNull Instant pickupStartAt,
    @NotNull Instant pickupEndAt,
    @NotBlank @Size(max = 2048) @JsonAlias("imageUrl") String imageAsset,
    UUID locationId,
    @Pattern(regexp = "EUR") String currency) {}
