package ro.wasteless.common.port;

import java.util.UUID;

public interface OfferStockPort {
  void reserve(UUID offerId, int quantity);

  void release(UUID offerId, int quantity);
}
