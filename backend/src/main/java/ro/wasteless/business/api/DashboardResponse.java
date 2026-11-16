package ro.wasteless.business.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardResponse(
    BigDecimal revenue,
    BigDecimal commission,
    BigDecimal netRevenue,
    long productsSold,
    BigDecimal co2AvoidedKg,
    long activeOffers,
    long pausedOffers,
    long activeOrders,
    long completedOrders,
    long canceledOrders,
    String currency,
    LocalDate from,
    LocalDate to) {}
