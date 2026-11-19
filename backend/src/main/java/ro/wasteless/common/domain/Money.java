package ro.wasteless.common.domain;

import java.math.*;

public final class Money {
  private Money() {}

  public static BigDecimal eur(BigDecimal value) {
    if (value == null || value.signum() < 0)
      throw new IllegalArgumentException("Money must be non-negative");
    return value.setScale(2, RoundingMode.UNNECESSARY);
  }
}
