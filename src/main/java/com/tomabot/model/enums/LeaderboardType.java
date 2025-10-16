package com.tomabot.model.enums;

import lombok.Getter;

/**
 * Types of leaderboards
 */
@Getter
public enum LeaderboardType {
    LEVEL("level", "Level", "👑", "Highest level achieved"),
    XP("xp", "Total XP", "⭐", "Total experience points earned"),
    SESSIONS("sessions", "Sessions", "🍅", "Total focus sessions completed"),
    FOCUS_TIME("focus_time", "Focus Time", "⏱️", "Total minutes of focus"),
    STREAK("streak", "Streak", "🔥", "Current consecutive days"),
    TASKS("tasks", "Tasks", "✅", "Total tasks completed"),
    ACHIEVEMENTS("achievements", "Achievements", "🏆", "Total badges unlocked");

    private final String key;
    private final String displayName;
    private final String emoji;
    private final String description;

    LeaderboardType(String key, String displayName, String emoji, String description) {
        this.key = key;
        this.displayName = displayName;
        this.emoji = emoji;
        this.description = description;
    }

    public String getFullName() {
        return emoji + " " + displayName;
    }

    /**
     * Get Redis key for this leaderboard type
     */
    public String getRedisKey(String scope) {
        return String.format("leaderboard:%s:%s", scope, key);
    }

    /**
     * Parse from string
     */
    public static LeaderboardType fromString(String type) {
        if (type == null) return LEVEL;

        for (LeaderboardType lb : values()) {
            if (lb.key.equalsIgnoreCase(type) || lb.name().equalsIgnoreCase(type)) {
                return lb;
            }
        }
        return LEVEL;
    }
}