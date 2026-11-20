package ro.wasteless;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import ro.wasteless.offer.infrastructure.OfferMaintenance;
import ro.wasteless.support.*;
import tools.jackson.databind.json.JsonMapper;

@Transactional
class CatalogIT extends PostgisIntegrationTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired WebApplicationContext context;
  @Autowired JsonMapper mapper;
  @Autowired OfferMaintenance maintenance;
  MockMvc mvc;
  Fixtures fixtures;
  UUID owner, business, location, category;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    fixtures = new Fixtures(jdbc);
    owner = fixtures.user();
    business = fixtures.business(owner);
    location = fixtures.location(business);
    category =
        jdbc.queryForObject(
            "insert into offer_categories(slug,display_name) values ('bakery','Bakery') on conflict(slug) do update set is_active=true returning id",
            UUID.class);
  }

  Map<String, Object> input() {
    return new HashMap<>(
        Map.of(
            "title",
            "Bakery bag",
            "category",
            "bakery",
            "description",
            "Surprise bag",
            "price",
            new BigDecimal("4.00"),
            "originalPrice",
            new BigDecimal("12.00"),
            "quantity",
            10,
            "pickupStartAt",
            TestFoundation.NOW.plusSeconds(3600).toString(),
            "pickupEndAt",
            TestFoundation.NOW.plusSeconds(7200).toString(),
            "imageAsset",
            "https://example.test/bag.png"));
  }

  @Test
  void consentedLocationFallbackAndUnavailableOffersAreFiltered() throws Exception {
    var id = create();
    mvc.perform(get("/api/v1/offers/nearby"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_LOCATION"));
    var client = fixtures.user();
    jdbc.update(
        "insert into user_locations(user_id,latitude,longitude) values (?,44.4268,26.1025)",
        client);
    mvc.perform(get("/api/v1/offers/nearby").with(user(client.toString()).roles("CLIENT")))
        .andExpect(status().isBadRequest());
    jdbc.update("update users set location_sharing_enabled=true where id=?", client);
    mvc.perform(get("/api/v1/offers/nearby").with(user(client.toString()).roles("CLIENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    for (var state : List.of("PAUSED", "SOLD_OUT", "EXPIRED")) {
      jdbc.update("update offers set status=? where id=?", state, id);
      mvc.perform(
              get("/api/v1/offers/nearby")
                  .param("latitude", "44.4268")
                  .param("longitude", "26.1025"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isEmpty());
    }
    jdbc.update("update offers set status='ACTIVE',quantity_available=0 where id=?", id);
    mvc.perform(get("/api/v1/offers/" + id)).andExpect(status().isNotFound());
    jdbc.update("update offers set quantity_available=10 where id=?", id);
    for (var params :
        List.of(
            Map.of("maxPrice", "3"),
            Map.of("minDiscount", "90"),
            Map.of("category", "drinks"),
            Map.of("query", "nonexistent"),
            Map.of("pickupBefore", "14:00"))) {
      var request =
          get("/api/v1/offers/nearby").param("latitude", "44.4268").param("longitude", "26.1025");
      params.forEach((key, value) -> request.param(key, value));
      mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }
  }

  UUID create() throws Exception {
    var result =
        mvc.perform(
                post("/api/v1/businesses/me/offers")
                    .with(user(owner.toString()).roles("BUSINESS"))
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(input())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.stockLevel").value("low"))
            .andExpect(jsonPath("$.available").value(10))
            .andReturn();
    return UUID.fromString(
        mapper.readTree(result.getResponse().getContentAsString()).get("id").asString());
  }

  @Test
  void crudPauseResumeSoftDeleteAndOwnership() throws Exception {
    var id = create();
    var path = "/api/v1/businesses/me/offers/" + id;
    mvc.perform(
            get("/api/v1/offers/nearby").param("latitude", "44.4268").param("longitude", "26.1025"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    var other = fixtures.user();
    fixtures.business(other);
    mvc.perform(
            patch(path)
                .with(user(other.toString()).roles("BUSINESS"))
                .contentType("application/json")
                .content("{\"title\":\"Stolen\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(get(path).with(user(owner.toString()).roles("CLIENT")))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch(path)
                .with(user(owner.toString()).roles("BUSINESS"))
                .contentType("application/json")
                .content("{\"title\":\"Updated bag\",\"quantity\":12}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated bag"))
        .andExpect(jsonPath("$.available").value(12));
    mvc.perform(post(path + "/pause").with(user(owner.toString()).roles("BUSINESS")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isPaused").value(true));
    mvc.perform(post(path + "/pause").with(user(owner.toString()).roles("BUSINESS")))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/offers/" + id)).andExpect(status().isNotFound());
    mvc.perform(post(path + "/resume").with(user(owner.toString()).roles("BUSINESS")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    mvc.perform(delete(path).with(user(owner.toString()).roles("BUSINESS")))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/offers/" + id)).andExpect(status().isNotFound());
    mvc.perform(get(path).with(user(owner.toString()).roles("BUSINESS")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DELETED"));
    mvc.perform(post(path + "/resume").with(user(owner.toString()).roles("BUSINESS")))
        .andExpect(status().isConflict());
  }

  @Test
  void validationAndExactBusinessPageContract() throws Exception {
    var bad = input();
    bad.put("price", 0);
    mvc.perform(
            post("/api/v1/businesses/me/offers")
                .with(user(owner.toString()).roles("BUSINESS"))
                .contentType("application/json")
                .content(mapper.writeValueAsString(bad)))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    create();
    var result =
        mvc.perform(
                get("/api/v1/businesses/me/offers").with(user(owner.toString()).roles("BUSINESS")))
            .andExpect(status().isOk())
            .andReturn();
    var page = mapper.readTree(result.getResponse().getContentAsString());
    var pageKeys = new HashSet<String>();
    page.propertyNames().forEach(pageKeys::add);
    assertThat(pageKeys)
        .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages");
    var keys = new HashSet<String>();
    var row = page.get("content").get(0);
    row.propertyNames().forEach(keys::add);
    assertThat(keys)
        .containsExactlyInAnyOrder(
            "id",
            "title",
            "imageAsset",
            "available",
            "price",
            "originalPrice",
            "stockLevel",
            "category",
            "description",
            "pickupWindow",
            "isPaused",
            "status");
    for (var key : List.of("available", "price", "originalPrice"))
      assertThat(row.get(key).isNumber()).isTrue();
    assertThat(row.get("isPaused").isBoolean()).isTrue();
  }

  @Test
  void geoFiltersDistancePaginationAndLocationUpdates() throws Exception {
    var id = create();
    mvc.perform(
            get("/api/v1/offers/nearby")
                .param("latitude", "44.4268")
                .param("longitude", "26.1025")
                .param("radiusKm", "0.5")
                .param("category", "bakery")
                .param("query", "bag")
                .param("minDiscount", "60")
                .param("maxPrice", "5")
                .param("pickupToday", "true")
                .param("pickupBefore", "17:00")
                .param("sort", "BIGGEST_DISCOUNT")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].distanceKm").value(0.0));
    for (var sort : List.of("NEAREST", "LOWEST_PRICE", "ENDING_SOON"))
      mvc.perform(
              get("/api/v1/offers/nearby")
                  .param("latitude", "44.4268")
                  .param("longitude", "26.1025")
                  .param("sort", sort)
                  .param("page", "1")
                  .param("size", "1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            patch("/api/v1/businesses/me/locations/" + location)
                .with(user(owner.toString()).roles("BUSINESS"))
                .contentType("application/json")
                .content("{\"latitude\":44.4368}"))
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/offers/nearby")
                .param("latitude", "44.4268")
                .param("longitude", "26.1025")
                .param("radiusKm", "0.5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());
    mvc.perform(
            get("/api/v1/offers/nearby")
                .param("latitude", "44.4268")
                .param("longitude", "26.1025")
                .param("radiusKm", "2"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[0].distanceKm").value(org.hamcrest.Matchers.closeTo(1.11, 0.03)));
    mvc.perform(get("/api/v1/businesses/" + business))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.phone").doesNotExist())
        .andExpect(jsonPath("$.primaryLocation.latitude").doesNotExist());
  }

  @Test
  void primaryUniquenessCrossBusinessLocationAndExpiry() throws Exception {
    create();
    var next =
        mvc.perform(
                post("/api/v1/businesses/me/locations")
                    .with(user(owner.toString()).roles("BUSINESS"))
                    .contentType("application/json")
                    .content(
                        "{\"addressLine1\":\"Second street\",\"city\":\"Bucharest\",\"countryCode\":\"RO\",\"latitude\":44.427,\"longitude\":26.103,\"isPrimary\":true}"))
            .andExpect(status().isCreated())
            .andReturn();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from business_locations where business_id=? and is_primary",
                Integer.class,
                business))
        .isEqualTo(1);
    var other = fixtures.user();
    var otherBusiness = fixtures.business(other);
    var otherLocation = fixtures.location(otherBusiness);
    var bad = input();
    bad.put("locationId", otherLocation.toString());
    mvc.perform(
            post("/api/v1/businesses/me/offers")
                .with(user(owner.toString()).roles("BUSINESS"))
                .contentType("application/json")
                .content(mapper.writeValueAsString(bad)))
        .andExpect(status().isUnprocessableContent());
    jdbc.update(
        "update offers set pickup_start_at=?,pickup_end_at=? where business_id=?",
        Timestamp.from(TestFoundation.NOW.minusSeconds(7200)),
        Timestamp.from(TestFoundation.NOW.minusSeconds(1)),
        business);
    maintenance.reconcile();
    maintenance.reconcile();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from offers where business_id=? and status='EXPIRED'",
                Integer.class,
                business))
        .isEqualTo(1);
    var nextId = mapper.readTree(next.getResponse().getContentAsString()).get("id").asString();
    mvc.perform(
            delete("/api/v1/businesses/me/locations/" + nextId)
                .with(user(owner.toString()).roles("BUSINESS")))
        .andExpect(status().isNoContent());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from business_locations where business_id=? and is_primary",
                Integer.class,
                business))
        .isEqualTo(1);
  }
}
