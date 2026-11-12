package ro.wasteless.dto;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;
import ro.wasteless.entities.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class Api {
    private Api() {}
    public record Register(@NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=72) String password, @NotNull Role role,
        @Size(max=200) String businessName, @Size(max=255) String pickupLocation) {
        public Register { email=normalizeEmail(email); }
    }
    public record Login(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) String password) {
        public Login { email=normalizeEmail(email); }
    }
    private static String normalizeEmail(String email) {
        return email==null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
    public record User(UUID id, String email, Role role, String businessName, String pickupLocation) {
        public static User of(UserAccount u) { return new User(u.id,u.email,u.role,u.businessName,u.pickupLocation); }
    }
    public record Auth(String accessToken, String tokenType, Instant expiresAt, User user) {}
    public record CreateOffer(@NotBlank @Size(max=200) String productName, @NotNull Offer.Category category,
        @NotBlank @Size(max=2000) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal originalPrice,
        @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal wasteLessPrice,
        @Pattern(regexp="[A-Z]{3}") String currency, @NotNull @Positive Integer quantity,
        @Size(max=255) String pickupLocation, @NotNull OffsetDateTime pickupStart,
        @NotNull OffsetDateTime pickupEnd, @URL(protocol="https") @Size(max=2048) String imageUrl) {}
    public record OfferView(UUID id, UUID businessId, String businessName, String productName,
        Offer.Category category, String description, BigDecimal originalPrice, BigDecimal wasteLessPrice,
        String currency, int quantity, String pickupLocation, Instant pickupStart, Instant pickupEnd,
        Instant createdAt, String imageUrl) {
        public static OfferView of(Offer o) { return new OfferView(o.id,o.business.id,o.business.businessName,
            o.productName,o.category,o.description,o.originalPrice,o.wasteLessPrice,o.currency,o.quantity,
            o.pickupLocation,o.pickupStart,o.pickupEnd,o.createdAt,o.imageUrl); }
    }
    public record CreateReservation(@NotNull UUID offerId, @NotNull @Positive Integer quantity) {}
    public record ReservationView(UUID id, UUID offerId, UUID clientId, int quantity, Instant createdAt,
        BigDecimal unitPrice, String currency, BigDecimal total, Reservation.Status status) {
        public static ReservationView of(Reservation r) { return new ReservationView(r.id,r.offer.id,r.client.id,
            r.quantity,r.createdAt,r.unitPrice,r.currency,r.total,r.status); }
    }
    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageView<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageView<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages());
        }
    }
}
