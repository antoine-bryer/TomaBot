package com.tomabot.service;

import com.tomabot.model.enums.LeaderboardScope;
import com.tomabot.model.enums.LeaderboardType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled job to refresh leaderboards periodically
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardRefreshJob {

    private final LeaderboardService leaderboardService;

    /**
     * Refresh global leaderboards every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at :00
    public void refreshGlobalLeaderboards() {
        log.info("Starting global leaderboard refresh...");

        try {
            for (LeaderboardType type : LeaderboardType.values()) {
                leaderboardService.rebuildLeaderboard(type, LeaderboardScope.GLOBAL, null);
            }

            log.info("Global leaderboards refreshed successfully");

        } catch (Exception e) {
            log.error("Error refreshing global leaderboards", e);
        }
    }

    /**
     * Cleanup old leaderboard entries at midnight
     */
    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void cleanupLeaderboards() {
        log.info("Starting leaderboard cleanup...");

        try {
            // Rebuild all leaderboards to remove inactive users
            for (LeaderboardType type : LeaderboardType.values()) {
                leaderboardService.rebuildLeaderboard(type, LeaderboardScope.GLOBAL, null);
            }

            log.info("Leaderboard cleanup completed");

        } catch (Exception e) {
            log.error("Error during leaderboard cleanup", e);
        }
    }
}