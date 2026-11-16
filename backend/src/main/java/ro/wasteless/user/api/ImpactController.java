package ro.wasteless.user.api;

import org.springframework.web.bind.annotation.*;
import ro.wasteless.user.service.ImpactService;

@RestController
@RequestMapping("/api/v1/users/me")
public class ImpactController {
  private final ImpactService impact;

  public ImpactController(ImpactService impact) {
    this.impact = impact;
  }

  @GetMapping("/impact")
  public ImpactResponse impact() {
    return impact.impact();
  }
}
