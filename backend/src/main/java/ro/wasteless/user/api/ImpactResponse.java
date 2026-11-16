package ro.wasteless.user.api;

import java.math.BigDecimal;

public record ImpactResponse(
    long mealsRescued,
    BigDecimal moneySaved,
    BigDecimal co2AvoidedKg,
    int level,
    long mealsToNextLevel,
    String currency) {}
