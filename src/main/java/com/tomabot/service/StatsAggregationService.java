package com.tomabot.service;

import com.tomabot.model.entity.User;
import com.tomabot.model.entity.UserStats;
import com.tomabot.repository.PomodoroSessionRepository;
import com.tomabot.repository.TaskRepository;
import com.tomabot.repository.UserRepository;
import com.tomabot.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

/**
 * Service for aggregating statistics periodically
 * Runs scheduled jobs to keep stats up-to-date
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsAggregationService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final PomodoroSessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final StatsService statsService;

    /**
     * Daily stats aggregation job
     * Runs every day at 00:05 AM
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void aggregateDailyStats() {
        log.info("Starting daily stats aggregation...");

        try {
            List<User> allUsers = userRepository.findAll();
            int updated = 0;

            for (User user : allUsers) {
                if (aggregateUserSafely(user)) {
                    updated++;
                }
            }

            log.info("Daily stats aggregation completed. Updated {} users", updated);

        } catch (Exception e) {
            log.error("Error during daily stats aggregation", e);
        }
    }

    private boolean aggregateUserSafely(User user) {
        try {
            aggregateUserStats(user); //NOSONAR
            return true;
        } catch (Exception e) {
            log.error("Failed to aggregate stats for user {}", user.getDiscordId(), e);
            return false;
        }
    }

    /**
     * Hourly streak check
     * Runs every hour to update streaks
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkStreaks() {
        log.debug("Checking user streaks...");

        try {
            List<UserStats> allStats = userStatsRepository.findAll();
            int updated = 0;

            LocalDate yesterday = LocalDate.now().minusDays(1);

            for (UserStats stats : allStats) {
                if (stats.getCurrentStreak() > 0 && stats.getLastSessionDate() != null) {
                    LocalDate lastDate = stats.getLastSessionDate()
                            .atZone(ZoneId.systemDefault()).toLocalDate();

                    // If last session was not yesterday or today, break streak
                    if (lastDate.isBefore(yesterday)) {
                        stats.updateStreak(0);
                        userStatsRepository.save(stats);
                        updated++;
                        log.debug("Broke streak for user {}", stats.getUser().getDiscordId());
                    }
                }
            }

            if (updated > 0) {
                log.info("Updated {} user streaks", updated);
            }

        } catch (Exception e) {
            log.error("Error during streak check", e);
        }
    }

    /**
     * Weekly cleanup job
     * Runs every Sunday at 01:00 AM
     */
    @Scheduled(cron = "0 0 1 * * SUN")
    @Transactional
    public void weeklyCleanup() {
        log.info("Starting weekly cleanup...");

        try {
            // Clear old Redis cache
            statsService.invalidateStatsCache("*");

            // Additional cleanup tasks can be added here

            log.info("Weekly cleanup completed");

        } catch (Exception e) {
            log.error("Error during weekly cleanup", e);
        }
    }

    /**
     * Aggregate all stats for a specific user
     */
    @Transactional
    public void aggregateUserStats(User user) {
        UserStats stats = userStatsRepository.findByUser(user)
                .orElseGet(() -> statsService.getOrCreateUserStats(user));

        // Calculate all-time totals
        Instant allTimeStart = Instant.ofEpochMilli(0);
        Instant now = Instant.now();

        Long completed = sessionRepository.countCompletedByUserAndDateRange(user, allTimeStart, now);
        Long interrupted = sessionRepository.countInterruptedByUserAndDateRange(user, allTimeStart, now);
        Integer focusMinutes = sessionRepository.sumFocusMinutesByUserAndDateRange(user, allTimeStart, now);
        Long tasksCompleted = taskRepository.countCompletedByUserAndDateRange(user, allTimeStart, now);

        // Update stats
        stats.setTotalSessionsCompleted(completed.intValue());
        stats.setTotalSessionsInterrupted(interrupted.intValue());
        stats.setTotalFocusMinutes(focusMinutes);
        stats.setTotalTasksCompleted(tasksCompleted.intValue());

        userStatsRepository.save(stats);

        log.debug("Aggregated stats for user {}: {} sessions, {} minutes",
                user.getDiscordId(), completed, focusMinutes);
    }

    /**
     * Recalculate all user stats (manual trigger)
     */
    @Transactional
    public void recalculateAllStats() {
        log.info("Recalculating all user stats...");

        List<User> allUsers = userRepository.findAll();
        int updated = 0;

        for (User user : allUsers) {
            try {
                aggregateUserStats(user); //NOSONAR
                statsService.invalidateStatsCache(user.getDiscordId());
                updated++;
            } catch (Exception e) {
                log.error("Failed to recalculate stats for user {}", user.getDiscordId(), e);
            }
        }

        log.info("Recalculated stats for {} users", updated);
    }

    /**
     * Get global platform statistics
     */
    @Transactional(readOnly = true)
    public GlobalStatsDTO getGlobalStats() {
        Long activeUsers = userStatsRepository.countActiveUsers();
        Double avgFocusMinutes = userStatsRepository.getAverageFocusMinutes();
        Long totalFocusMinutes = userStatsRepository.getTotalFocusMinutes();

        return GlobalStatsDTO.builder()
                .totalActiveUsers(activeUsers != null ? activeUsers.intValue() : 0)
                .averageFocusMinutes(avgFocusMinutes != null ? avgFocusMinutes.intValue() : 0)
                .totalFocusMinutes(totalFocusMinutes != null ? totalFocusMinutes.intValue() : 0)
                .build();
    }

    /**
     * DTO for global platform statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class GlobalStatsDTO {
        private Integer totalActiveUsers;
        private Integer averageFocusMinutes;
        private Integer totalFocusMinutes;
    }
}