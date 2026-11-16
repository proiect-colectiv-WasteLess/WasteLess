package ro.wasteless.user.service;

import java.util.List;

public final class ImpactLevels {
  private static final List<Long> STARTS = List.of(0L, 10L, 25L, 50L, 100L);

  private ImpactLevels() {}

  public static int level(long meals) {
    int level = 1;
    while (level < STARTS.size() && meals >= STARTS.get(level)) level++;
    return level;
  }

  public static long remaining(long meals) {
    int level = level(meals);
    return level == STARTS.size() ? 0 : STARTS.get(level) - meals;
  }
}
