package ro.wasteless;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import ro.wasteless.offer.infrastructure.DealMapper;
import tools.jackson.databind.json.JsonMapper;

class DealContractTest {
  @Test
  void immutableSnapshotMatchesFlutterKeysTypesAndBucharestTime() throws Exception {
    var rs = mock(ResultSet.class);
    var id = UUID.randomUUID();
    when(rs.getObject("id", UUID.class)).thenReturn(id);
    when(rs.getString("title")).thenReturn("Bakery bag", "Renamed later");
    when(rs.getString("business_name")).thenReturn("Fresh Bakery");
    when(rs.getBigDecimal("price")).thenReturn(new BigDecimal("4.00"));
    when(rs.getDouble("distance_m")).thenReturn(1234.567);
    when(rs.getInt("quantity_available")).thenReturn(3);
    when(rs.getBigDecimal("rating_avg")).thenReturn(new BigDecimal("4.90"));
    when(rs.getTimestamp("pickup_start_at"))
        .thenReturn(Timestamp.from(Instant.parse("2026-07-20T14:30:00Z")));
    when(rs.getTimestamp("pickup_end_at"))
        .thenReturn(Timestamp.from(Instant.parse("2026-07-20T16:00:00Z")));
    when(rs.getString("image_url")).thenReturn("/media/images/bag.png");
    when(rs.getString("slug")).thenReturn("fruit-and-veg");
    var mapper = new DealMapper("https://wasteless.example");
    var response = mapper.mapRow(rs, 0);
    var next = mapper.mapRow(rs, 0);
    assertThat(response.name()).isEqualTo("Bakery bag");
    assertThat(next.name()).isEqualTo("Renamed later");
    assertThat(response.oldPrice()).isEqualByComparingTo("4.00");
    assertThat(response.distanceKm()).isEqualByComparingTo("1.23");
    assertThat(response.pickupStart()).isEqualTo("17:30");
    assertThat(response.pickupEnd()).isEqualTo("19:00");
    assertThat(response.category()).isEqualTo("fruitAndVeg");
    assertThat(response.description()).isEmpty();
    assertThat(response.imageAsset()).isEqualTo("https://wasteless.example/media/images/bag.png");
    var json = JsonMapper.builder().build().valueToTree(response);
    var names = new HashSet<String>();
    json.propertyNames().forEach(names::add);
    assertThat(names)
        .containsExactlyInAnyOrder(
            "id",
            "name",
            "shop",
            "oldPrice",
            "price",
            "distanceKm",
            "pickupStart",
            "pickupEnd",
            "left",
            "rating",
            "imageAsset",
            "category",
            "description");
    for (var field : List.of("oldPrice", "price", "distanceKm", "left", "rating"))
      assertThat(json.get(field).isNumber()).isTrue();
    for (var field :
        List.of(
            "id",
            "name",
            "shop",
            "pickupStart",
            "pickupEnd",
            "imageAsset",
            "category",
            "description")) assertThat(json.get(field).isString()).isTrue();
  }
}
