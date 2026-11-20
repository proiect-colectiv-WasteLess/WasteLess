package ro.wasteless.offer.infrastructure;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.wasteless.offer.domain.OfferCategory;

public interface OfferCategoryRepository extends JpaRepository<OfferCategory, UUID> {
  List<OfferCategory> findByIsActiveTrueOrderBySortOrderAscSlugAsc();

  Optional<OfferCategory> findBySlugAndIsActiveTrue(String slug);
}
