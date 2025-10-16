package com.tomabot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single leaderboard entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO {

    private Integer rank;
    private String discordId;
    private String username;
    private Double score;

    // Additional context based on leaderboard type
    private Integer level;
    private Integer totalXP;
    private Integer sessions;
    private Integer focusMinutes;
    private Integer streak;
    private Integer tasks;
    private Integer achievements;

    private Boolean isCurrentUser;

    /**
     * Get medal emoji for top 3
     */
    public String getMedalEmoji() {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "  ";
        };
    }

    /**
     * Get formatted rank display
     */
    public String getRankDisplay() {
        if (rank <= 3) {
            return getMedalEmoji();
        }
        return String.format("#%d", rank);
    }

    /**
     * Format score based on type
     */
    public String getFormattedScore(String type) {
        if (score == null) return "0";

        return switch (type.toLowerCase()) {
            case "level" -> String.format("Level %d", score.intValue());
            case "xp" -> String.format("%,d XP", score.intValue());
            case "sessions" -> String.format("%d sessions", score.intValue());
            case "focus_time" -> formatDuration(score.intValue());
            case "streak" -> String.format("%d days", score.intValue());
            case "tasks" -> String.format("%d tasks", score.intValue());
            case "achievements" -> String.format("%d badges", score.intValue());
            default -> String.format("%.0f", score);
        };
    }

    /**
     * Format duration in human-readable format
     */
    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int remainingMins = minutes % 60;

        if (hours < 24) {
            return String.format("%dh %02dm", hours, remainingMins);
        }

        int days = hours / 24;
        int remainingHours = hours % 24;
        return String.format("%dd %dh", days, remainingHours);
    }
}