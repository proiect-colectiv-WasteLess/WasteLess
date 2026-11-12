package ro.wasteless.services;
import ro.wasteless.dto.Api;
import ro.wasteless.entities.*;
import ro.wasteless.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Service
@Transactional(readOnly=true)
public class MarketplaceService {
    private final UserRepository users;
    private final OfferRepository offers;
    private final ReservationRepository reservations;
    public MarketplaceService(UserRepository users, OfferRepository offers, ReservationRepository reservations) {
        this.users=users; this.offers=offers; this.reservations=reservations;
    }
    public UserAccount actor(UUID id, Role role) {
        UserAccount u=users.findById(id).orElseThrow(()->new ApiException(401,"Account no longer exists"));
        if(u.role!=role) throw new ApiException(403,"Role not permitted");
        return u;
    }
    private Pageable page(int page, int size) {
        if(page<0 || size<1 || size>100) throw new ApiException(400,"Page must be >= 0 and size between 1 and 100");
        return PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt").and(Sort.by("id")));
    }
    @Transactional
    public Api.OfferView publish(UUID id, Api.CreateOffer r) {
        UserAccount business=actor(id,Role.BUSINESS);
        if(r.wasteLessPrice().compareTo(r.originalPrice())>0) throw new ApiException(400,"WasteLess price exceeds original price");
        if(!r.pickupStart().toInstant().isBefore(r.pickupEnd().toInstant()) || !r.pickupEnd().toInstant().isAfter(Instant.now()))
            throw new ApiException(400,"Pickup start must precede end, and end must be in the future");
        String currency=r.currency()==null ? "EUR" : r.currency();
        try { Currency.getInstance(currency); } catch(IllegalArgumentException e) { throw new ApiException(400,"Unknown ISO 4217 currency"); }
        Offer o=new Offer(); o.business=business; o.productName=r.productName().strip(); o.category=r.category();
        o.description=r.description().strip(); o.originalPrice=r.originalPrice(); o.wasteLessPrice=r.wasteLessPrice();
        o.currency=currency; o.quantity=r.quantity();
        o.pickupLocation=r.pickupLocation()==null || r.pickupLocation().isBlank() ? business.pickupLocation : r.pickupLocation().strip();
        o.pickupStart=r.pickupStart().toInstant(); o.pickupEnd=r.pickupEnd().toInstant(); o.imageUrl=r.imageUrl();
        return Api.OfferView.of(offers.save(o));
    }
    public Api.PageView<Api.OfferView> available(UUID id,int page,int size) {
        actor(id,Role.CLIENT);
        return Api.PageView.of(offers.findByQuantityGreaterThanAndPickupEndAfter(0,Instant.now(),page(page,size)).map(Api.OfferView::of));
    }
    public Api.OfferView offer(UUID id,UUID offerId) {
        UserAccount u=users.findById(id).orElseThrow(()->new ApiException(401,"Account no longer exists"));
        Offer o=offers.findById(offerId).orElseThrow(()->new ApiException(404,"Offer not found"));
        if(u.role==Role.BUSINESS && !o.business.id.equals(id)) throw new ApiException(403,"Offer belongs to another business");
        return Api.OfferView.of(o);
    }
    public Api.PageView<Api.OfferView> ownOffers(UUID id,int page,int size) {
        actor(id,Role.BUSINESS);
        return Api.PageView.of(offers.findByBusinessId(id,page(page,size)).map(Api.OfferView::of));
    }
    @Transactional
    public Api.ReservationView reserve(UUID id,Api.CreateReservation r) {
        UserAccount client=actor(id,Role.CLIENT);
        // SELECT ... FOR UPDATE serializes competing reservations until commit.
        Offer o=offers.lockById(r.offerId()).orElseThrow(()->new ApiException(404,"Offer not found"));
        // Check time AFTER acquiring the lock: waiting requests may cross pickupEnd.
        if(!o.pickupEnd.isAfter(Instant.now())) throw new ApiException(409,"Offer pickup window has ended");
        if(o.quantity<r.quantity()) throw new ApiException(409,"Insufficient stock");
        o.quantity-=r.quantity();
        Reservation reservation=new Reservation(); reservation.client=client; reservation.offer=o;
        reservation.quantity=r.quantity(); reservation.unitPrice=o.wasteLessPrice; reservation.currency=o.currency;
        reservation.total=o.wasteLessPrice.multiply(BigDecimal.valueOf(r.quantity()));
        return Api.ReservationView.of(reservations.save(reservation));
    }
    public Api.ReservationView reservation(UUID id,UUID reservationId) {
        UserAccount u=users.findById(id).orElseThrow(()->new ApiException(401,"Account no longer exists"));
        Reservation r=reservations.findById(reservationId).orElseThrow(()->new ApiException(404,"Reservation not found"));
        boolean allowed=u.role==Role.CLIENT ? r.client.id.equals(id) : r.offer.business.id.equals(id);
        if(!allowed) throw new ApiException(403,"Reservation belongs to another account");
        return Api.ReservationView.of(r);
    }
    public Api.PageView<Api.ReservationView> ownReservations(UUID id,int page,int size,Role role) {
        actor(id,role);
        Page<Reservation> result=role==Role.CLIENT ? reservations.findByClientId(id,page(page,size))
            : reservations.findByOfferBusinessId(id,page(page,size));
        return Api.PageView.of(result.map(Api.ReservationView::of));
    }
}