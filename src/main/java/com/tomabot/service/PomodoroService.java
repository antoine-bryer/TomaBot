package com.tomabot.service;

import com.tomabot.model.dto.SessionStatus;
import com.tomabot.model.entity.PomodoroSession;
import com.tomabot.model.entity.User;
import com.tomabot.model.enums.SessionType;
import com.tomabot.repository.PomodoroSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PomodoroService {

    private final PomodoroSessionRepository sessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SchedulerService schedulerService;
    private final StatsService statsService; // NEW: Stats integration

    private static final String ACTIVE_SESSION_KEY = "session:active:";

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

        // Store in Redis for quick access (with error handling)
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
        String key = ACTIVE_SESSION_KEY + user.getDiscordId();
        redisTemplate.delete(key);

        // Cancel scheduled job
        schedulerService.cancelSessionJob(sessionId);

        log.info("Stopped session {} for user {}", sessionId, user.getDiscordId());
    }

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

        // Remove from Redis
        String key = ACTIVE_SESSION_KEY + discordId;
        redisTemplate.delete(key);

        log.info("Completed session {} for user {}", sessionId, discordId);
    }

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

    public boolean hasActiveSession(User user) {
        return getActiveSessionId(user) != null;
    }

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