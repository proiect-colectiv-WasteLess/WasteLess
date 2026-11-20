package ro.wasteless.business.api;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.business.domain.BusinessLocation;

@RestController
@RequestMapping("/api/v1/businesses/me")
@PreAuthorize("hasRole('BUSINESS')")
public class LocationController {
  private final LocationService locations;
  private final tools.jackson.databind.json.JsonMapper mapper;
  private final jakarta.validation.Validator validator;

  public LocationController(
      LocationService locations,
      tools.jackson.databind.json.JsonMapper mapper,
      jakarta.validation.Validator validator) {
    this.locations = locations;
    this.mapper = mapper;
    this.validator = validator;
  }

  @GetMapping("/locations")
  public List<BusinessLocation> list() {
    return locations.list();
  }

  @PostMapping("/locations")
  @ResponseStatus(HttpStatus.CREATED)
  public BusinessLocation create(@Valid @RequestBody LocationRequest r) {
    return locations.save(null, r, false);
  }

  @PatchMapping("/locations/{id}")
  public BusinessLocation update(@PathVariable UUID id, @RequestBody Map<String, Object> patch) {
    var old =
        locations.list().stream()
            .filter(l -> l.id.equals(id))
            .findFirst()
            .orElseThrow(BusinessAccess::notFound);
    // Merge fields before Bean Validation, retaining PATCH semantics for omitted values.
    var values = new java.util.LinkedHashMap<String, Object>(mapper.convertValue(old, Map.class));
    for (var key : patch.keySet())
      if (!Set.of(
              "label",
              "addressLine1",
              "addressLine2",
              "city",
              "postalCode",
              "countryCode",
              "latitude",
              "longitude",
              "isPrimary")
          .contains(key)) throw new CatalogValidationException(key, "Unknown location field");
    values
        .keySet()
        .retainAll(
            Set.of(
                "label",
                "addressLine1",
                "addressLine2",
                "city",
                "postalCode",
                "countryCode",
                "latitude",
                "longitude",
                "isPrimary"));
    values.putAll(patch);
    LocationRequest r;
    try {
      r = mapper.convertValue(values, LocationRequest.class);
    } catch (IllegalArgumentException e) {
      throw new CatalogValidationException("location", "Invalid value");
    }
    var errors = validator.validate(r);
    if (!errors.isEmpty()) {
      var e = errors.iterator().next();
      throw new CatalogValidationException(e.getPropertyPath().toString(), e.getMessage());
    }
    return locations.save(id, r, false);
  }

  @PutMapping("/location")
  public BusinessLocation primary(@Valid @RequestBody LocationRequest r) {
    return locations.save(null, r, true);
  }

  @DeleteMapping("/locations/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    locations.delete(id);
  }
}
