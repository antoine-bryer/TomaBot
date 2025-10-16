package com.tomabot.model.enums;

/**
 * Types of achievement requirements
 */
public enum AchievementType {
    SESSIONS_COMPLETED,      // Total sessions completed
    TOTAL_FOCUS_MINUTES,     // Total focus time
    STREAK_DAYS,             // Consecutive days streak
    TASKS_COMPLETED,         // Total tasks completed
    LEVEL_REACHED,           // Reach a specific level
    MORNING_SESSIONS,        // Sessions before 12pm
    EVENING_SESSIONS,        // Sessions after 10pm
    WEEKEND_SESSIONS,        // Sessions on weekend
    SPECIAL_DATE,            // Specific date (Christmas, etc.)
    PERFECT_WEEK,            // 7 days 100% completion
    SPEED_RUN,               // Multiple sessions in one day
    EARLY_ADOPTER,           // Joined early
    MARATHON,                // Long single session
    TASK_MASTER             // Many tasks in one day
}