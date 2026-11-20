package ro.wasteless.offer.api;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import ro.wasteless.offer.infrastructure.OfferCategoryRepository;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
  private final OfferCategoryRepository categories;

  public CategoryController(OfferCategoryRepository categories) {
    this.categories = categories;
  }

  @GetMapping
  public List<CategoryResponse> list() {
    return categories.findByIsActiveTrueOrderBySortOrderAscSlugAsc().stream()
        .map(c -> new CategoryResponse(c.getId(), c.getSlug(), c.getDisplayName()))
        .toList();
  }

  public record CategoryResponse(UUID id, String slug, String displayName) {}
}
