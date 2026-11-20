package ro.wasteless;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import ro.wasteless.business.api.CatalogValidationException;
import ro.wasteless.offer.api.*;
import tools.jackson.databind.json.JsonMapper;

class OfferValidationTest {
  final Instant now = Instant.parse("2026-11-20T12:00:00Z");
  final JsonMapper mapper = JsonMapper.builder().build();

  Map<String, Object> valid() {
    return new HashMap<>(
        Map.of(
            "title",
            "Bakery bag",
            "category",
            "bakery",
            "price",
            new BigDecimal("4.00"),
            "originalPrice",
            new BigDecimal("12.00"),
            "quantity",
            10,
            "pickupStartAt",
            now.plusSeconds(3600).toString(),
            "pickupEndAt",
            now.plusSeconds(7200).toString(),
            "imageAsset",
            "https://example.test/bag.png"));
  }

  @Test
  void validBoundaryValuesPass() {
    var values = valid();
    values.put("quantity", 10000);
    values.put("pickupEndAt", now.plusSeconds(3600 + 86400).toString());
    assertThatCode(
            () -> OfferRules.validateValues(mapper.convertValue(values, OfferInput.class), now))
        .doesNotThrowAnyException();
  }

  @Test
  void invalidBoundariesReturnFieldErrors() {
    for (var change :
        List.of(
            Map.entry("title", "   "),
            Map.entry("title", "ab"),
            Map.entry("quantity", 0),
            Map.entry("quantity", -1),
            Map.entry("quantity", 10001),
            Map.entry("price", 0),
            Map.entry("price", -1),
            Map.entry("price", 1.001),
            Map.entry("originalPrice", 1),
            Map.entry("currency", "USD"),
            Map.entry("pickupStartAt", now.toString()),
            Map.entry("pickupEndAt", now.plusSeconds(90001).toString()),
            Map.entry("imageAsset", "file:///etc/passwd"),
            Map.entry("imageAsset", "javascript:alert(1)"),
            Map.entry("imageAsset", "https://user:pass@example.test/x"))) {
      var values = valid();
      values.put(change.getKey(), change.getValue());
      assertThatThrownBy(
              () -> OfferRules.validateValues(mapper.convertValue(values, OfferInput.class), now))
          .as(change.toString())
          .isInstanceOf(CatalogValidationException.class);
    }
  }
}
