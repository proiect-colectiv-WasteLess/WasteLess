package ro.wasteless;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.wasteless.business.api.*;
import ro.wasteless.common.api.*;
import ro.wasteless.offer.api.*;
import ro.wasteless.offer.infrastructure.*;
import ro.wasteless.support.TestFoundation;

class CatalogHttpTest {
  @org.springframework.boot.test.context.TestConfiguration
  @Import(CatalogSecurity.class)
  static class SecurityTestConfig {
    @Bean
    BusinessOfferService offers() {
      return mock(BusinessOfferService.class);
    }

    @Bean
    BusinessOfferController controller(BusinessOfferService service) {
      return new BusinessOfferController(service);
    }
  }

  @Test
  void businessRoleIsRequiredBeforeServiceRuns() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(SecurityTestConfig.class)) {
      var controller = context.getBean(BusinessOfferController.class);
      var service = context.getBean(BusinessOfferService.class);
      try (var ignored = TestFoundation.asUser(UUID.randomUUID(), "CLIENT")) {
        assertThatThrownBy(() -> controller.get(UUID.randomUUID()))
            .isInstanceOf(AccessDeniedException.class);
      }
      verifyNoInteractions(service);
      var id = UUID.randomUUID();
      try (var ignored = TestFoundation.asUser(UUID.randomUUID(), "BUSINESS")) {
        controller.get(id);
      }
      verify(service).get(id);
    }
  }

  @Test
  void unknownCategoriesProduce422FieldErrorsAndMissingLocationProduces400() throws Exception {
    var service = mock(BusinessOfferService.class);
    when(service.create(any()))
        .thenThrow(new CatalogValidationException("category", "Unknown category"));
    var mvc =
        MockMvcBuilders.standaloneSetup(new BusinessOfferController(service))
            .setControllerAdvice(new CatalogExceptionHandler(), new GlobalExceptionHandler())
            .build();
    mvc.perform(
            post("/api/v1/businesses/me/offers")
                .contentType("application/json")
                .content(
                    """
        {"title":"Bakery bag","category":"invalid","price":4,"originalPrice":12,"quantity":10,
         "pickupStartAt":"2026-11-21T12:00:00Z","pickupEndAt":"2026-11-21T13:00:00Z","imageAsset":"https://example.test/x.png"}
        """))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
    var discovery = mock(DiscoveryService.class);
    when(discovery.nearby(any()))
        .thenThrow(
            new DomainException(
                ErrorCode.MISSING_LOCATION,
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Provide a location"));
    mvc =
        MockMvcBuilders.standaloneSetup(new DiscoveryController(discovery))
            .setControllerAdvice(new CatalogExceptionHandler(), new GlobalExceptionHandler())
            .build();
    mvc.perform(get("/api/v1/offers/nearby"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_LOCATION"));
  }

  @Test
  void discoveryRejectsUnboundedRequests() {
    for (var request :
        List.of(
            new DiscoveryRequest(0d, 0d, 51, null, null, null, null, null, false, "NEAREST", 0, 20),
            new DiscoveryRequest(
                0d, 0d, Double.NaN, null, null, null, null, null, false, "NEAREST", 0, 20),
            new DiscoveryRequest(0d, 0d, 1, null, null, null, null, null, false, "NEAREST", 0, 51),
            new DiscoveryRequest(
                0d,
                0d,
                1,
                null,
                null,
                BigDecimal.valueOf(101),
                null,
                null,
                false,
                "NEAREST",
                0,
                20)))
      assertThatThrownBy(() -> DiscoveryService.validate(request))
          .isInstanceOf(CatalogValidationException.class);
  }
}
