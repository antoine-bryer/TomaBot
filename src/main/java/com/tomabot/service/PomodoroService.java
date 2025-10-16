package com.tomabot.service;

import com.tomabot.model.dto.SessionStatus;
import com.tomabot.model.entity.PomodoroSession;
import com.tomabot.model.entity.User;
import com.tomabot.model.enums.SessionType;
import com.tomabot.model.enums.XPSource;
import com.tomabot.repository.PomodoroSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Pomodoro sessions
 */
@Service
@Slf4j
public class PomodoroService {

    private final PomodoroSessionRepository sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SchedulerService schedulerService;
    private final StatsService statsService;

    private ExperienceService experienceService;
    private AchievementService achievementService;
    private LeaderboardService leaderboardService;

    private static final String ACTIVE_SESSION_KEY = "session:active:";

    // Constructor avec dépendances obligatoires
    public PomodoroService(PomodoroSessionRepository sessionRepository,
                           RedisTemplate<String, Object> redisTemplate,
                           SchedulerService schedulerService,
                           StatsService statsService) {
        this.sessionRepository = sessionRepository;
        this.redisTemplate = redisTemplate;
        this.schedulerService = schedulerService;
        this.statsService = statsService;
    }

    // Setters pour dépendances circulaires avec @Lazy
    @Autowired
    public void setExperienceService(@Lazy ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @Autowired
    public void setAchievementService(@Lazy AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @Autowired
    public void setLeaderboardService(@Lazy LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Start a new Pomodoro session
     */
    @Transactional
    public PomodoroSession startSession(User user, Integer durationMinutes) {
        // Check if already has active session
        try {
            if (hasActiveSession(user)) {
                throw new IllegalStateException("You already have an active session! Use /stop first.");
            }
        } catch (Exception e) {
            log.warn("Redis check failed, continuing without cache: {}", e.getMessage());
        }

        Instant now = Instant.now();
        Instant endTime = now.plusSeconds(durationMinutes * 60L);

        // Create session entity
        PomodoroSession session = PomodoroSession.builder()
                .user(user)
                .sessionType(SessionType.FOCUS)
                .durationMinutes(durationMinutes)
                .startTime(now)
                .completed(false)
                .interrupted(false)
                .build();

        session = sessionRepository.save(session);

        // Store in Redis for quick access
        try {
            String key = ACTIVE_SESSION_KEY + user.getDiscordId();
            redisTemplate.opsForValue().set(key, session.getId(), durationMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache session in Redis: {}", e.getMessage());
        }

        // Schedule completion job
        schedulerService.scheduleSessionCompletion(session.getId(), user.getDiscordId(), endTime);

        log.info("Started session {} for user {}", session.getId(), user.getDiscordId());
        return session;
    }

    /**
     * Stop an active session (interrupt)
     */
    @Transactional
    public void stopSession(User user) {
        Long sessionId = getActiveSessionId(user);

        if (sessionId == null) {
            throw new IllegalStateException("No active session found!");
        }

        PomodoroSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session not found!"));

        session.interrupt();
        sessionRepository.save(session);

        // Update stats after interruption
        statsService.updateStatsAfterSession(user, session);

        // Remove from Redis
        try {
            String key = ACTIVE_SESSION_KEY + user.getDiscordId();
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to remove session from Redis: {}", e.getMessage());
        }

        // Cancel scheduled job
        schedulerService.cancelSessionJob(sessionId);

        log.info("Stopped session {} for user {}", sessionId, user.getDiscordId());
    }

    /**
     * Complete a session (called by scheduler)
     */
    @Transactional
    public void completeSession(Long sessionId, String discordId) {
        PomodoroSession session = sessionRepository.findById(sessionId)
                .orElse(null);

        if (session == null) {
            log.warn("Session {} not found for completion", sessionId);
            return;
        }

        session.complete();
        sessionRepository.save(session);

        // Update stats after completion
        statsService.updateStatsAfterSession(session.getUser(), session);

        // Grant XP and check achievements
        grantSessionRewards(session.getUser(), session);

        // Remove from Redis
        try {
            String key = ACTIVE_SESSION_KEY + discordId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to remove session from Redis: {}", e.getMessage());
        }

        log.info("Completed session {} for user {}", sessionId, discordId);
    }

    /**
     * Grant XP, check achievements, and update leaderboards after session completion
     */
    private void grantSessionRewards(User user, PomodoroSession session) {
        try {
            // Base XP = duration in minutes (25 min = 25 XP)
            int xpAmount = session.getDurationMinutes();

            // Grant session XP
            if (experienceService != null) {
                experienceService.grantXP(user, XPSource.SESSION_COMPLETED, xpAmount, session.getId());

                // Check for first session bonus
                if (experienceService.shouldGrantFirstSessionBonus(user)) {
                    experienceService.grantXP(user, XPSource.FIRST_SESSION_OF_DAY, session.getId());
                    log.info("Granted first session bonus to user {}", user.getDiscordId());
                }
            }

            // Check for achievement unlocks
            if (achievementService != null) {
                achievementService.checkAndUnlockAchievements(user);
            }

            // Update leaderboards
            if (leaderboardService != null) {
                leaderboardService.updateUserLeaderboards(user, null);
            }

        } catch (Exception e) {
            log.error("Failed to grant session rewards to user {}", user.getDiscordId(), e);
        }
    }

    /**
     * Get session status for user
     */
    public SessionStatus getSessionStatus(User user) {
        Long sessionId = getActiveSessionId(user);

        if (sessionId == null) {
            return null;
        }

        PomodoroSession session = sessionRepository.findById(sessionId).orElse(null);

        if (session == null) {
            return null;
        }

        Instant now = Instant.now();
        long elapsedSeconds = Duration.between(session.getStartTime(), now).getSeconds();
        long totalSeconds = session.getDurationMinutes() * 60L;
        long remainingSeconds = totalSeconds - elapsedSeconds;

        if (remainingSeconds < 0) remainingSeconds = 0;

        return SessionStatus.builder()
                .sessionId(session.getId())
                .totalMinutes(session.getDurationMinutes())
                .elapsedMinutes((int) (elapsedSeconds / 60))
                .remainingMinutes((int) (remainingSeconds / 60))
                .startTime(session.getStartTime())
                .build();
    }

    /**
     * Check if user has an active session
     */
    public boolean hasActiveSession(User user) {
        return getActiveSessionId(user) != null;
    }

    /**
     * Get active session ID for user
     */
    private Long getActiveSessionId(User user) {
        try {
            String key = ACTIVE_SESSION_KEY + user.getDiscordId();
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : null;
        } catch (Exception e) {
            log.warn("Redis unavailable, checking database for active session");
            // Fallback: check database
            return sessionRepository.findByUserAndCompletedAndInterrupted(user, false, false)
                    .map(PomodoroSession::getId)
                    .orElse(null);
        }
    }
}