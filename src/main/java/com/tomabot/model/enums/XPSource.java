package com.tomabot.model.enums;

import lombok.Getter;

/**
 * Sources of XP gains
 */
@Getter
public enum XPSource {
    SESSION_COMPLETED(25, "Completed a focus session"),
    TASK_COMPLETED(10, "Completed a task"),
    STREAK_BONUS(100, "Maintained a 7-day streak"),
    FIRST_SESSION_OF_DAY(5, "First session of the day"),
    ACHIEVEMENT_UNLOCKED(50, "Unlocked an achievement"),
    DAILY_LOGIN(2, "Daily login bonus"),
    MANUAL_GRANT(0, "Manually granted by admin");

    /**
     * -- GETTER --
     *  Get default XP amount for this source
     */
    private final int defaultAmount;
    private final String description;

    XPSource(int defaultAmount, String description) {
        this.defaultAmount = defaultAmount;
        this.description = description;
    }

    /**
     * Get XP amount based on context (e.g., session duration)
     */
    public int getAmount(int... params) {
        return switch (this) {
            case SESSION_COMPLETED -> params.length > 0 ? params[0] : defaultAmount;
            case TASK_COMPLETED, STREAK_BONUS, FIRST_SESSION_OF_DAY,
                 ACHIEVEMENT_UNLOCKED, DAILY_LOGIN, MANUAL_GRANT -> defaultAmount;
        };
    }

    /**
     * Get formatted description with amount
     */
    public String getFormattedDescription(int amount) {
        return String.format("%s (+%d XP)", description, amount);
    }
}