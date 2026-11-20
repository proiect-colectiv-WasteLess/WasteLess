package ro.wasteless.offer.infrastructure;

import java.time.Clock;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import ro.wasteless.common.api.*;
import ro.wasteless.common.port.OfferStockPort;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class OfferStockAdapter implements OfferStockPort {
  private final OfferRepository offers;
  private final Clock clock;

  public OfferStockAdapter(OfferRepository offers, Clock clock) {
    this.offers = offers;
    this.clock = clock;
  }

  @Override
  public void reserve(UUID offerId, int quantity) {
    var offer =
        offers
            .lockById(offerId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.OFFER_NOT_AVAILABLE,
                        HttpStatus.CONFLICT,
                        "Offer is not available"));
    offer.reserve(quantity, clock.instant());
  }

  @Override
  public void release(UUID offerId, int quantity) {
    restore(offerId, quantity);
  }

  public void restore(UUID offerId, int quantity) {
    if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
    var offer =
        offers
            .lockById(offerId)
            .orElseThrow(
                () ->
                    new DomainException(
                        ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "Offer not found"));
    if (offer.deletedAt != null)
      LoggerFactory.getLogger(getClass())
          .warn("Stock restoration ignored for deleted offer {}", offerId);
    offer.restore(quantity, clock.instant());
  }
}
