package com.tomabot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for user statistics across different time periods
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {

    // Basic stats
    private Long userId;
    private String username;
    private String period; // today, week, month, all

    // Focus time
    private Integer totalFocusMinutes;
    private Integer averageFocusPerDay;

    // Sessions
    private Integer sessionsCompleted;
    private Integer sessionsTotal;
    private Integer sessionsInterrupted;
    private Integer completionRate; // percentage

    // Tasks
    private Integer tasksCompleted;
    private Integer tasksActive;
    private Integer tasksTotal;

    // Streaks
    private Integer currentStreak;
    private Integer bestStreak;
    private LocalDate streakStartDate;

    // Trends
    private Double trendPercentage; // vs previous period
    private Boolean isImproving;

    // Breakdown by day (for week/month views)
    private int[] dailyBreakdown; // sessions per day
    private Map<LocalDate, Integer> dailyFocusMinutes;

    // Level & XP (Phase 2)
    private Integer level;
    private Integer currentXP;
    private Integer xpToNextLevel;

    // Achievements count
    private Integer achievementsUnlocked;
    private Integer achievementsTotal;

    // Time distribution
    private Integer morningSessionsCount; // before 12pm
    private Integer afternoonSessionsCount; // 12pm-6pm
    private Integer eveningSessionsCount; // after 6pm

    // Best day
    private LocalDate mostProductiveDay;
    private Integer mostProductiveDayMinutes;

    /**
     * Calculate completion rate from sessions
     */
    public void calculateCompletionRate() {
        if (sessionsTotal != null && sessionsTotal > 0) {
            this.completionRate = (int) ((double) sessionsCompleted / sessionsTotal * 100);
        } else {
            this.completionRate = 0;
        }
    }

    /**
     * Check if user is improving based on trend
     */
    public void calculateIsImproving() {
        this.isImproving = trendPercentage != null && trendPercentage > 0;
    }

    /**
     * Get total sessions (completed + interrupted)
     */
    public Integer getTotalSessionsStarted() {
        return (sessionsCompleted != null ? sessionsCompleted : 0) +
                (sessionsInterrupted != null ? sessionsInterrupted : 0);
    }
}