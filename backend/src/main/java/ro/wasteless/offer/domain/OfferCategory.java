package ro.wasteless.offer.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "offer_categories")
public class OfferCategory {
  @Id private UUID id;
  private String slug;
  private String displayName;
  private int sortOrder;
  private boolean isActive;

  protected OfferCategory() {}

  public UUID getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static String frontendName(String slug) {
    return "fruit-and-veg".equals(slug) ? "fruitAndVeg" : slug;
  }
}
