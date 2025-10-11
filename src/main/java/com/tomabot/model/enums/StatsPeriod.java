package com.tomabot.model.enums;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Time periods for statistics aggregation
 */
public enum StatsPeriod {
    TODAY,
    WEEK,
    MONTH,
    ALL_TIME;

    /**
     * Get start date for this period
     */
    public LocalDate getStartDate() {
        LocalDate now = LocalDate.now();
        return switch (this) {
            case TODAY -> now;
            case WEEK -> now.minusDays(6); // Last 7 days including today
            case MONTH -> now.minusDays(29); // Last 30 days including today
            case ALL_TIME -> LocalDate.of(2024, 1, 1); // Project start
        };
    }

    /**
     * Get end date for this period (always today)
     */
    public LocalDate getEndDate() {
        return LocalDate.now();
    }

    /**
     * Get number of days in this period
     */
    public long getDayCount() {
        return ChronoUnit.DAYS.between(getStartDate(), getEndDate()) + 1;
    }

    /**
     * Get previous period start date (for trend calculation)
     */
    public LocalDate getPreviousPeriodStart() {
        LocalDate start = getStartDate();
        long days = getDayCount();
        return start.minusDays(days);
    }

    /**
     * Get previous period end date
     */
    public LocalDate getPreviousPeriodEnd() {
        return getStartDate().minusDays(1);
    }

    /**
     * Parse from string
     */
    public static StatsPeriod fromString(String period) {
        return switch (period.toLowerCase()) {
            case "today" -> TODAY;
            case "week" -> WEEK;
            case "month" -> MONTH;
            case "all", "alltime", "all_time" -> ALL_TIME;
            default -> TODAY;
        };
    }

    /**
     * Get display name
     */
    public String getDisplayName() {
        return switch (this) {
            case TODAY -> "Today";
            case WEEK -> "This Week";
            case MONTH -> "This Month";
            case ALL_TIME -> "All Time";
        };
    }
}