package ro.wasteless.business.api;

import java.util.List;
import java.util.Map;

public class CatalogValidationException extends RuntimeException {
  private final List<Map<String, String>> fields;

  public CatalogValidationException(String field, String message) {
    super("Invalid request");
    fields = List.of(Map.of("field", field, "message", message));
  }

  public List<Map<String, String>> fields() {
    return fields;
  }
}
