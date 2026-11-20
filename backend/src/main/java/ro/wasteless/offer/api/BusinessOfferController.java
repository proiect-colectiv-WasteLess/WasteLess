package ro.wasteless.offer.api;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/businesses/me/offers")
@PreAuthorize("hasRole('BUSINESS')")
public class BusinessOfferController {
  private final BusinessOfferService offers;

  public BusinessOfferController(BusinessOfferService offers) {
    this.offers = offers;
  }

  @GetMapping
  public PageResponse<BusinessOfferResponse> list(
      @RequestParam(defaultValue = "ALL") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return offers.list(status, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BusinessOfferResponse create(@Valid @RequestBody OfferInput r) {
    return offers.create(r);
  }

  @GetMapping("/{id}")
  public BusinessOfferResponse get(@PathVariable UUID id) {
    return offers.get(id);
  }

  @PutMapping("/{id}")
  public BusinessOfferResponse replace(@PathVariable UUID id, @Valid @RequestBody OfferInput r) {
    return offers.replace(id, r);
  }

  @PatchMapping("/{id}")
  public BusinessOfferResponse patch(
      @PathVariable UUID id, @RequestBody Map<String, Object> patch) {
    return offers.patch(id, patch);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    offers.delete(id);
  }
}
