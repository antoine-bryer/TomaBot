package com.tomabot.service;

import com.tomabot.model.dto.UserStatsDTO;
import com.tomabot.model.entity.PomodoroSession;
import com.tomabot.model.entity.User;
import com.tomabot.model.entity.UserStats;
import com.tomabot.model.enums.StatsPeriod;
import com.tomabot.repository.PomodoroSessionRepository;
import com.tomabot.repository.TaskRepository;
import com.tomabot.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final PomodoroSessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final UserStatsRepository userStatsRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STATS_CACHE_KEY = "stats:cache:";
    private static final long CACHE_TTL_MINUTES = 5;

    /**
     * Get user statistics for a specific period
     */
    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(User user, String periodStr) {
        // Try cache first
        String cacheKey = STATS_CACHE_KEY + user.getDiscordId() + ":" + periodStr;
        UserStatsDTO cached = getCachedStats(cacheKey);
        if (cached != null) {
            log.debug("Returning cached stats for user {}", user.getDiscordId());
            return cached;
        }

        StatsPeriod period = StatsPeriod.fromString(periodStr);
        UserStatsDTO stats = calculateStats(user, period);

        // Cache result
        cacheStats(cacheKey, stats);

        return stats;
    }

    /**
     * Calculate statistics for a user and period
     */
    private UserStatsDTO calculateStats(User user, StatsPeriod period) {
        Instant startDate = period.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endDate = period.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Fetch aggregate user stats
        UserStats userStats = userStatsRepository.findByUser(user)
                .orElse(createDefaultUserStats(user));

        UserStatsDTO.UserStatsDTOBuilder builder = UserStatsDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .period(period.name());

        // Calculate period-specific stats
        if (period == StatsPeriod.ALL_TIME) {
            return buildAllTimeStats(builder, user, userStats);
        } else {
            return buildPeriodStats(builder, user, userStats, startDate, endDate, period);
        }
    }

    /**
     * Build all-time statistics
     */
    private UserStatsDTO buildAllTimeStats(UserStatsDTO.UserStatsDTOBuilder builder,
                                           User user, UserStats userStats) {
        builder
                .totalFocusMinutes(userStats.getTotalFocusMinutes())
                .sessionsCompleted(userStats.getTotalSessionsCompleted())
                .sessionsTotal(userStats.getTotalSessions())
                .sessionsInterrupted(userStats.getTotalSessionsInterrupted())
                .tasksCompleted(userStats.getTotalTasksCompleted())
                .currentStreak(userStats.getCurrentStreak())
                .bestStreak(userStats.getBestStreak())
                .level(userStats.getLevel())
                .currentXP(userStats.getCurrentXp())
                .xpToNextLevel(calculateXpForLevel(userStats.getLevel() + 1))
                .achievementsUnlocked(userStats.getAchievementsCount())
                .trendPercentage(0.0);

        // Calculate average per day (since first session)
        Long daysSinceStart = calculateDaysSinceFirstSession(user);
        if (daysSinceStart > 0) {
            int avgPerDay = (int) (userStats.getTotalFocusMinutes() / daysSinceStart);
            builder.averageFocusPerDay(avgPerDay);
        }

        UserStatsDTO stats = builder.build();
        stats.calculateCompletionRate();
        stats.calculateIsImproving();

        return stats;
    }

    /**
     * Build period-specific statistics
     */
    private UserStatsDTO buildPeriodStats(UserStatsDTO.UserStatsDTOBuilder builder,
                                          User user, UserStats userStats,
                                          Instant startDate, Instant endDate,
                                          StatsPeriod period) {
        // Count sessions
        Long completed = sessionRepository.countCompletedByUserAndDateRange(user, startDate, endDate);
        Long total = sessionRepository.countByUserAndDateRange(user, startDate, endDate);
        Long interrupted = sessionRepository.countInterruptedByUserAndDateRange(user, startDate, endDate);

        // Sum focus minutes
        Integer focusMinutes = sessionRepository.sumFocusMinutesByUserAndDateRange(user, startDate, endDate);

        // Count tasks
        Long tasksCompleted = taskRepository.countCompletedByUserAndDateRange(user, startDate, endDate);
        Long tasksActive = taskRepository.countActiveTasksByUser(user);

        // Calculate trend
        Double trend = calculateTrend(user, period, focusMinutes);

        // Build daily breakdown
        int[] dailyBreakdown = buildDailyBreakdown(user, startDate, endDate, period);
        Map<LocalDate, Integer> dailyFocusMap = buildDailyFocusMap(user, startDate, endDate);

        // Time distribution
        Long morning = sessionRepository.countMorningSessions(user, startDate, endDate);
        Long afternoon = sessionRepository.countAfternoonSessions(user, startDate, endDate);
        Long evening = sessionRepository.countEveningSessions(user, startDate, endDate);

        // Most productive day
        LocalDate mostProductiveDay = findMostProductiveDay(user, startDate, endDate);
        Integer mostProductiveDayMinutes = mostProductiveDay != null ?
                dailyFocusMap.getOrDefault(mostProductiveDay, 0) : 0;

        // Calculate average per day
        long dayCount = period.getDayCount();
        int avgPerDay = dayCount > 0 ? (int) (focusMinutes / dayCount) : 0;

        builder
                .totalFocusMinutes(focusMinutes)
                .averageFocusPerDay(avgPerDay)
                .sessionsCompleted(completed.intValue())
                .sessionsTotal(total.intValue())
                .sessionsInterrupted(interrupted.intValue())
                .tasksCompleted(tasksCompleted.intValue())
                .tasksActive(tasksActive.intValue())
                .currentStreak(userStats.getCurrentStreak())
                .bestStreak(userStats.getBestStreak())
                .trendPercentage(trend)
                .dailyBreakdown(dailyBreakdown)
                .dailyFocusMinutes(dailyFocusMap)
                .morningSessionsCount(morning.intValue())
                .afternoonSessionsCount(afternoon.intValue())
                .eveningSessionsCount(evening.intValue())
                .mostProductiveDay(mostProductiveDay)
                .mostProductiveDayMinutes(mostProductiveDayMinutes)
                .level(userStats.getLevel())
                .currentXP(userStats.getCurrentXp())
                .xpToNextLevel(calculateXpForLevel(userStats.getLevel() + 1))
                .achievementsUnlocked(userStats.getAchievementsCount());

        UserStatsDTO stats = builder.build();
        stats.calculateCompletionRate();
        stats.calculateIsImproving();

        return stats;
    }

    /**
     * Calculate trend percentage compared to previous period
     */
    private Double calculateTrend(User user, StatsPeriod period, Integer currentMinutes) {
        if (period == StatsPeriod.ALL_TIME) {
            return 0.0;
        }

        Instant prevStart = period.getPreviousPeriodStart().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant prevEnd = period.getPreviousPeriodEnd().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Integer previousMinutes = sessionRepository.sumFocusMinutesByUserAndDateRange(user, prevStart, prevEnd);

        if (previousMinutes == 0) {
            return currentMinutes > 0 ? 100.0 : 0.0;
        }

        double change = ((double) currentMinutes - previousMinutes) / previousMinutes * 100;
        return Math.round(change * 10.0) / 10.0; // Round to 1 decimal
    }

    /**
     * Build daily breakdown array for visualization
     */
    private int[] buildDailyBreakdown(User user, Instant startDate, Instant endDate, StatsPeriod period) {
        List<Object[]> dailyCounts = sessionRepository.countSessionsByDate(user, startDate, endDate);

        int days = (int) period.getDayCount();
        int[] breakdown = new int[Math.min(days, 30)]; // Max 30 days for display

        Map<LocalDate, Integer> countMap = dailyCounts.stream()
                .collect(Collectors.toMap(
                        row -> convertToLocalDate(row[0]), // FIX: Conversion SQL Date -> LocalDate
                        row -> ((Long) row[1]).intValue()
                ));

        LocalDate start = period.getStartDate();
        for (int i = 0; i < breakdown.length; i++) {
            LocalDate date = start.plusDays(i);
            breakdown[i] = countMap.getOrDefault(date, 0);
        }

        return breakdown;
    }

    /**
     * Build daily focus minutes map
     */
    private Map<LocalDate, Integer> buildDailyFocusMap(User user, Instant startDate, Instant endDate) {
        List<Object[]> dailyMinutes = sessionRepository.sumFocusMinutesByDate(user, startDate, endDate);

        return dailyMinutes.stream()
                .collect(Collectors.toMap(
                        row -> convertToLocalDate(row[0]), // FIX: Conversion SQL Date -> LocalDate
                        row -> ((Long) row[1]).intValue()
                ));
    }

    /**
     * Find most productive day in period
     */
    private LocalDate findMostProductiveDay(User user, Instant startDate, Instant endDate) {
        List<Object[]> dailyMinutes = sessionRepository.sumFocusMinutesByDate(user, startDate, endDate);

        return dailyMinutes.stream()
                .max(Comparator.comparing(row -> (Long) row[1]))
                .map(row -> convertToLocalDate(row[0])) // FIX: Conversion SQL Date -> LocalDate
                .orElse(null);
    }

    /**
     * Convert java.sql.Date or java.time.LocalDate to LocalDate
     * Handles both types returned by different database drivers
     */
    private LocalDate convertToLocalDate(Object dateObject) {
        if (dateObject == null) {
            return null;
        }

        if (dateObject instanceof LocalDate localDate) {
            return localDate;
        }

        if (dateObject instanceof java.sql.Date date) {
            return date.toLocalDate();
        }

        throw new IllegalArgumentException("Cannot convert " + dateObject.getClass() + " to LocalDate");
    }

    /**
     * Calculate XP required for a level
     */
    private Integer calculateXpForLevel(int level) {
        return level * level * 50;
    }

    /**
     * Calculate days since first session
     */
    private Long calculateDaysSinceFirstSession(User user) {
        List<PomodoroSession> allSessions = sessionRepository.findByUserOrderByStartTimeDesc(user);

        if (allSessions.isEmpty()) {
            return 0L;
        }

        PomodoroSession firstSession = allSessions.get(allSessions.size() - 1);
        LocalDate firstDate = firstSession.getStartTime().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();

        return ChronoUnit.DAYS.between(firstDate, today) + 1;
    }

    /**
     * Create default UserStats for new user
     */
    private UserStats createDefaultUserStats(User user) {
        return UserStats.builder()
                .user(user)
                .totalFocusMinutes(0)
                .totalSessionsCompleted(0)
                .totalSessionsInterrupted(0)
                .totalTasksCompleted(0)
                .currentStreak(0)
                .bestStreak(0)
                .level(1)
                .currentXp(0)
                .totalXpEarned(0)
                .achievementsCount(0)
                .build();
    }

    /**
     * Get or create UserStats for user
     */
    @Transactional
    public UserStats getOrCreateUserStats(User user) {
        return userStatsRepository.findByUser(user)
                .orElseGet(() -> {
                    UserStats stats = createDefaultUserStats(user);
                    return userStatsRepository.save(stats);
                });
    }

    /**
     * Update user stats after session completion
     */
    @Transactional
    public void updateStatsAfterSession(User user, PomodoroSession session) {
        UserStats stats = getOrCreateUserStats(user); //NOSONAR

        if (Boolean.TRUE.equals(session.getCompleted())) {
            stats.incrementSessionsCompleted();
            stats.addFocusMinutes(session.getDurationMinutes());
        } else if (Boolean.TRUE.equals(session.getInterrupted())) {
            stats.incrementSessionsInterrupted();
        }

        stats.setLastSessionDate(session.getStartTime());

        // Update streak
        updateStreak(stats, session.getStartTime());

        userStatsRepository.save(stats);

        // Invalidate cache
        invalidateStatsCache(user.getDiscordId());

        log.debug("Updated stats for user {}", user.getDiscordId());
    }

    /**
     * Update user stats after task completion
     */
    @Transactional
    public void updateStatsAfterTaskCompletion(User user) {
        UserStats stats = getOrCreateUserStats(user); //NOSONAR
        stats.incrementTasksCompleted();
        userStatsRepository.save(stats);

        // Invalidate cache
        invalidateStatsCache(user.getDiscordId());
    }

    /**
     * Update streak based on last session date
     */
    private void updateStreak(UserStats stats, Instant sessionTime) {
        LocalDate sessionDate = sessionTime.atZone(ZoneId.systemDefault()).toLocalDate();

        if (stats.getLastSessionDate() == null) {
            // First session
            stats.updateStreak(1);
            return;
        }

        LocalDate lastDate = stats.getLastSessionDate().atZone(ZoneId.systemDefault()).toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(lastDate, sessionDate);

        if (daysBetween == 1) {
            // Consecutive day, increment
            stats.updateStreak(stats.getCurrentStreak() + 1);
        } else if(daysBetween != 0) {
            // Streak broken, reset
            stats.updateStreak(1);
        }
    }

    /**
     * Get cached stats
     */
    private UserStatsDTO getCachedStats(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            return cached != null ? (UserStatsDTO) cached : null;
        } catch (Exception e) {
            log.warn("Failed to get cached stats: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Cache stats
     */
    private void cacheStats(String cacheKey, UserStatsDTO stats) {
        try {
            redisTemplate.opsForValue().set(cacheKey, stats, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache stats: {}", e.getMessage());
        }
    }

    /**
     * Invalidate all cached stats for user
     */
    public void invalidateStatsCache(String discordId) {
        try {
            String pattern = STATS_CACHE_KEY + discordId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("Invalidated {} cached stats for user {}", keys.size(), discordId);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate stats cache: {}", e.getMessage());
        }
    }
}