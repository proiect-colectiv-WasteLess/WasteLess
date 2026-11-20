package ro.wasteless.offer.infrastructure;

import java.math.*;
import java.net.URI;
import java.sql.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ro.wasteless.offer.api.DealResponse;
import ro.wasteless.offer.domain.*;

@Component
public class DealMapper implements RowMapper<DealResponse> {
  private final URI baseUrl;

  public DealMapper(@Value("${app.public-base-url:http://localhost:8080}") String baseUrl) {
    this.baseUrl = URI.create(baseUrl + "/");
    if (!Set.of("http", "https").contains(this.baseUrl.getScheme())
        || this.baseUrl.getHost() == null)
      throw new IllegalArgumentException("PUBLIC_BASE_URL must be an absolute HTTP(S) URL");
  }

  @Override
  public DealResponse mapRow(ResultSet rs, int row) throws SQLException {
    var price = rs.getBigDecimal("price");
    var oldPrice = rs.getBigDecimal("original_price");
    String image = rs.getString("image_url");
    if (image == null || image.isBlank()) image = "/media/placeholder.png";
    image = baseUrl.resolve(image).toString();
    return new DealResponse(
        rs.getObject("id", UUID.class),
        rs.getString("title"),
        rs.getString("business_name"),
        oldPrice == null ? price : oldPrice,
        price,
        BigDecimal.valueOf(rs.getDouble("distance_m") / 1000).setScale(2, RoundingMode.HALF_UP),
        Offer.time(rs.getTimestamp("pickup_start_at").toInstant()),
        Offer.time(rs.getTimestamp("pickup_end_at").toInstant()),
        rs.getInt("quantity_available"),
        rs.getBigDecimal("rating_avg"),
        image,
        OfferCategory.frontendName(rs.getString("slug")),
        Optional.ofNullable(rs.getString("description")).orElse(""));
  }
}
