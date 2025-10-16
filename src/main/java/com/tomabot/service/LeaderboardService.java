package com.tomabot.service;

import com.tomabot.model.dto.LeaderboardEntryDTO;
import com.tomabot.model.entity.User;
import com.tomabot.model.entity.UserStats;
import com.tomabot.model.enums.LeaderboardScope;
import com.tomabot.model.enums.LeaderboardType;
import com.tomabot.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing leaderboards using Redis Sorted Sets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserStatsRepository userStatsRepository;

    private static final int TOP_LIMIT = 100; // Cache top 100 users
    private static final long CACHE_TTL_HOURS = 1; // Refresh every hour

    /**
     * Update user's position in all leaderboards
     */
    @Transactional(readOnly = true)
    public void updateUserLeaderboards(User user, String guildId) {
        UserStats stats = userStatsRepository.findByUser(user).orElse(null);
        if (stats == null) {
            log.warn("No stats found for user {}", user.getDiscordId());
            return;
        }

        try {
            String userId = user.getDiscordId();

            // Update global leaderboards
            updateLeaderboard(LeaderboardType.LEVEL, LeaderboardScope.GLOBAL, null,
                    userId, stats.getLevel());
            updateLeaderboard(LeaderboardType.XP, LeaderboardScope.GLOBAL, null,
                    userId, stats.getTotalXpEarned());
            updateLeaderboard(LeaderboardType.SESSIONS, LeaderboardScope.GLOBAL, null,
                    userId, stats.getTotalSessionsCompleted());
            updateLeaderboard(LeaderboardType.FOCUS_TIME, LeaderboardScope.GLOBAL, null,
                    userId, stats.getTotalFocusMinutes());
            updateLeaderboard(LeaderboardType.STREAK, LeaderboardScope.GLOBAL, null,
                    userId, stats.getCurrentStreak());
            updateLeaderboard(LeaderboardType.TASKS, LeaderboardScope.GLOBAL, null,
                    userId, stats.getTotalTasksCompleted());
            updateLeaderboard(LeaderboardType.ACHIEVEMENTS, LeaderboardScope.GLOBAL, null,
                    userId, stats.getAchievementsCount());

            // Update server leaderboards if guildId provided
            if (guildId != null) {
                updateLeaderboard(LeaderboardType.LEVEL, LeaderboardScope.SERVER, guildId,
                        userId, stats.getLevel());
                updateLeaderboard(LeaderboardType.XP, LeaderboardScope.SERVER, guildId,
                        userId, stats.getTotalXpEarned());
                updateLeaderboard(LeaderboardType.SESSIONS, LeaderboardScope.SERVER, guildId,
                        userId, stats.getTotalSessionsCompleted());
                updateLeaderboard(LeaderboardType.FOCUS_TIME, LeaderboardScope.SERVER, guildId,
                        userId, stats.getTotalFocusMinutes());
                updateLeaderboard(LeaderboardType.STREAK, LeaderboardScope.SERVER, guildId,
                        userId, stats.getCurrentStreak());
                updateLeaderboard(LeaderboardType.TASKS, LeaderboardScope.SERVER, guildId,
                        userId, stats.getTotalTasksCompleted());
                updateLeaderboard(LeaderboardType.ACHIEVEMENTS, LeaderboardScope.SERVER, guildId,
                        userId, stats.getAchievementsCount());
            }

            log.debug("Updated leaderboards for user {}", userId);

        } catch (Exception e) {
            log.error("Failed to update leaderboards for user {}", user.getDiscordId(), e);
        }
    }

    /**
     * Update a specific leaderboard entry
     */
    private void updateLeaderboard(LeaderboardType type, LeaderboardScope scope,
                                   String guildId, String userId, Number score) {
        try {
            String key = buildRedisKey(type, scope, guildId);
            double scoreValue = score != null ? score.doubleValue() : 0.0;

            redisTemplate.opsForZSet().add(key, userId, scoreValue);

            // Set expiration
            redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);

        } catch (Exception e) {
            log.warn("Failed to update leaderboard {} for user {}: {}",
                    type.getKey(), userId, e.getMessage());
        }
    }

    /**
     * Get top N entries from leaderboard
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getTopLeaderboard(LeaderboardType type,
                                                       LeaderboardScope scope,
                                                       String guildId,
                                                       int limit) {
        try {
            String key = buildRedisKey(type, scope, guildId);

            // Get top entries (reverse order = highest first)
            Set<ZSetOperations.TypedTuple<Object>> topEntries =
                    redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

            if (topEntries == null || topEntries.isEmpty()) {
                log.debug("No entries in leaderboard {}, rebuilding...", key);
                rebuildLeaderboard(type, scope, guildId);
                topEntries = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);
            }

            return buildLeaderboardEntries(topEntries, type, null);

        } catch (Exception e) {
            log.error("Error retrieving leaderboard {}: {}", type.getKey(), e.getMessage());
            return List.of();
        }
    }

    /**
     * Get user's rank and surrounding entries
     */
    @Transactional(readOnly = true)
    public LeaderboardEntryDTO getUserRank(LeaderboardType type,
                                           LeaderboardScope scope,
                                           String guildId,
                                           String userId) {
        try {
            String key = buildRedisKey(type, scope, guildId);

            Long rank = redisTemplate.opsForZSet().reverseRank(key, userId);
            Double score = redisTemplate.opsForZSet().score(key, userId);

            if (rank == null || score == null) {
                return null;
            }

            // Fetch user stats for additional info
            UserStats stats = userStatsRepository.findByDiscordId(userId).orElse(null);

            return LeaderboardEntryDTO.builder()
                    .rank(rank.intValue() + 1) // Redis rank is 0-based
                    .discordId(userId)
                    .username(stats != null ? stats.getUser().getUsername() : "Unknown")
                    .score(score)
                    .level(stats != null ? stats.getLevel() : 0)
                    .totalXP(stats != null ? stats.getTotalXpEarned() : 0)
                    .sessions(stats != null ? stats.getTotalSessionsCompleted() : 0)
                    .focusMinutes(stats != null ? stats.getTotalFocusMinutes() : 0)
                    .streak(stats != null ? stats.getCurrentStreak() : 0)
                    .tasks(stats != null ? stats.getTotalTasksCompleted() : 0)
                    .achievements(stats != null ? stats.getAchievementsCount() : 0)
                    .isCurrentUser(true)
                    .build();

        } catch (Exception e) {
            log.error("Error getting user rank for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Rebuild leaderboard from database
     */
    @Transactional(readOnly = true)
    public void rebuildLeaderboard(LeaderboardType type, LeaderboardScope scope, String guildId) {
        try {
            String key = buildRedisKey(type, scope, guildId);

            // Clear existing data
            redisTemplate.delete(key);

            // Fetch all user stats
            List<UserStats> allStats = userStatsRepository.findAll();

            // Add to sorted set
            for (UserStats stats : allStats) {
                double score = getScoreForType(type, stats);
                if (score > 0) {
                    redisTemplate.opsForZSet().add(key, stats.getUser().getDiscordId(), score);
                }
            }

            // Set expiration
            redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);

            log.info("Rebuilt leaderboard {} with {} entries", key, allStats.size());

        } catch (Exception e) {
            log.error("Error rebuilding leaderboard {}: {}", type.getKey(), e.getMessage());
        }
    }

    /**
     * Build leaderboard entries from Redis results
     */
    private List<LeaderboardEntryDTO> buildLeaderboardEntries(
            Set<ZSetOperations.TypedTuple<Object>> entries,
            LeaderboardType type,
            String currentUserId) {

        List<LeaderboardEntryDTO> result = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<Object> entry : entries) {
            String userId = entry.getValue().toString();
            Double score = entry.getScore();

            UserStats stats = userStatsRepository.findByDiscordId(userId).orElse(null);

            if (stats != null) {
                LeaderboardEntryDTO dto = LeaderboardEntryDTO.builder()
                        .rank(rank)
                        .discordId(userId)
                        .username(stats.getUser().getUsername())
                        .score(score)
                        .level(stats.getLevel())
                        .totalXP(stats.getTotalXpEarned())
                        .sessions(stats.getTotalSessionsCompleted())
                        .focusMinutes(stats.getTotalFocusMinutes())
                        .streak(stats.getCurrentStreak())
                        .tasks(stats.getTotalTasksCompleted())
                        .achievements(stats.getAchievementsCount())
                        .isCurrentUser(userId.equals(currentUserId))
                        .build();

                result.add(dto);
            }

            rank++;
        }

        return result;
    }

    /**
     * Get score for a specific leaderboard type
     */
    private double getScoreForType(LeaderboardType type, UserStats stats) {
        return switch (type) {
            case LEVEL -> stats.getLevel();
            case XP -> stats.getTotalXpEarned();
            case SESSIONS -> stats.getTotalSessionsCompleted();
            case FOCUS_TIME -> stats.getTotalFocusMinutes();
            case STREAK -> stats.getCurrentStreak();
            case TASKS -> stats.getTotalTasksCompleted();
            case ACHIEVEMENTS -> stats.getAchievementsCount();
        };
    }

    /**
     * Build Redis key for leaderboard
     */
    private String buildRedisKey(LeaderboardType type, LeaderboardScope scope, String guildId) {
        String scopeKey = scope == LeaderboardScope.SERVER && guildId != null
                ? "server:" + guildId
                : "global";
        return String.format("leaderboard:%s:%s", scopeKey, type.getKey());
    }

    /**
     * Get total number of users in leaderboard
     */
    public Long getLeaderboardSize(LeaderboardType type, LeaderboardScope scope, String guildId) {
        try {
            String key = buildRedisKey(type, scope, guildId);
            return redisTemplate.opsForZSet().size(key);
        } catch (Exception e) {
            log.error("Error getting leaderboard size: {}", e.getMessage());
            return 0L;
        }
    }
}