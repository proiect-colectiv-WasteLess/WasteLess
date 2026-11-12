package ro.wasteless.controllers;
import ro.wasteless.dto.Api;
import ro.wasteless.entities.Role;
import ro.wasteless.services.MarketplaceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import java.util.UUID;
@RestController @RequestMapping("/api/v1")
public class MarketplaceController {
    private final MarketplaceService market;
    public MarketplaceController(MarketplaceService market) { this.market=market; }
    private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @PostMapping("/offers") @ResponseStatus(HttpStatus.CREATED)
    public Api.OfferView publish(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Api.CreateOffer r) { return market.publish(id(jwt),r); }
    @GetMapping("/offers")
    public Api.PageView<Api.OfferView> offers(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return market.available(id(jwt),page,size); }
    @GetMapping("/offers/{offerId}")
    public Api.OfferView offer(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID offerId) { return market.offer(id(jwt),offerId); }
    @GetMapping("/business/offers")
    public Api.PageView<Api.OfferView> ownOffers(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return market.ownOffers(id(jwt),page,size); }
    @PostMapping("/reservations") @ResponseStatus(HttpStatus.CREATED)
    public Api.ReservationView reserve(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Api.CreateReservation r) { return market.reserve(id(jwt),r); }
    @GetMapping("/reservations/me")
    public Api.PageView<Api.ReservationView> ownReservations(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return market.ownReservations(id(jwt),page,size,Role.CLIENT); }
    @GetMapping("/reservations/{reservationId}")
    public Api.ReservationView reservation(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID reservationId) {
        return market.reservation(id(jwt),reservationId);
    }
    @GetMapping("/business/reservations")
    public Api.PageView<Api.ReservationView> businessReservations(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return market.ownReservations(id(jwt),page,size,Role.BUSINESS); }
}