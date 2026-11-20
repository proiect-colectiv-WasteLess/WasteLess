package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ro.wasteless.offer.domain.OfferCategory;

class CategoryTest {
  @Test
  void frontendNamesAreStable() {
    assertThat(OfferCategory.frontendName("fruit-and-veg")).isEqualTo("fruitAndVeg");
    assertThat(OfferCategory.frontendName("bakery")).isEqualTo("bakery");
    assertThat(OfferCategory.frontendName("other")).isEqualTo("other");
  }
}
