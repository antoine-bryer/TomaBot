package com.tomabot.model.enums;

import lombok.Getter;

/**
 * Scope of leaderboard (global or per server)
 */
@Getter
public enum LeaderboardScope {
    GLOBAL("global", "🌍 Global", "All TomaBot users worldwide"),
    SERVER("server", "🏠 Server", "Users in this Discord server only");

    private final String key;
    private final String displayName;
    private final String description;

    LeaderboardScope(String key, String displayName, String description) {
        this.key = key;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get Redis key suffix for this scope
     */
    public String getRedisKeySuffix(String guildId) {
        return switch (this) {
            case GLOBAL -> "global";
            case SERVER -> "server:" + guildId;
        };
    }

    /**
     * Parse from string
     */
    public static LeaderboardScope fromString(String scope) {
        if (scope == null) return GLOBAL;

        for (LeaderboardScope s : values()) {
            if (s.key.equalsIgnoreCase(scope) || s.name().equalsIgnoreCase(scope)) {
                return s;
            }
        }
        return GLOBAL;
    }
}