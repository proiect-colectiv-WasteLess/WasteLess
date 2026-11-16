package ro.wasteless.order;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ro.wasteless.user.service.ImpactLevels;

class ImpactLevelsTest {
  @Test
  void thresholdsAndMaximum() {
    long[] starts = {0, 10, 25, 50, 100};
    for (int i = 0; i < starts.length; i++) {
      assertThat(ImpactLevels.level(starts[i])).isEqualTo(i + 1);
      if (i > 0) assertThat(ImpactLevels.level(starts[i] - 1)).isEqualTo(i);
    }
    assertThat(ImpactLevels.remaining(0)).isEqualTo(10);
    assertThat(ImpactLevels.remaining(24)).isEqualTo(1);
    assertThat(ImpactLevels.remaining(1000)).isZero();
  }
}
