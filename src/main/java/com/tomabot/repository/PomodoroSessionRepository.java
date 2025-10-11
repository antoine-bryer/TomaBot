package com.tomabot.repository;

import com.tomabot.model.entity.PomodoroSession;
import com.tomabot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, Long> {

    Optional<PomodoroSession> findByUserAndCompletedAndInterrupted(
            User user, Boolean completed, Boolean interrupted);

    List<PomodoroSession> findByUserOrderByStartTimeDesc(User user);

    /**
     * Count sessions by user in date range
     */
    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate")
    Long countByUserAndDateRange(@Param("user") User user,
                                 @Param("startDate") Instant startDate,
                                 @Param("endDate") Instant endDate);

    /**
     * Count completed sessions by user in date range
     */
    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.completed = true AND s.startTime >= :startDate AND s.startTime < :endDate")
    Long countCompletedByUserAndDateRange(@Param("user") User user,
                                          @Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    /**
     * Count interrupted sessions by user in date range
     */
    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.interrupted = true AND s.startTime >= :startDate AND s.startTime < :endDate")
    Long countInterruptedByUserAndDateRange(@Param("user") User user,
                                            @Param("startDate") Instant startDate,
                                            @Param("endDate") Instant endDate);

    /**
     * Sum focus minutes by user in date range
     */
    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM PomodoroSession s " +
            "WHERE s.user = :user AND s.completed = true " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate")
    Integer sumFocusMinutesByUserAndDateRange(@Param("user") User user,
                                              @Param("startDate") Instant startDate,
                                              @Param("endDate") Instant endDate);

    /**
     * Get sessions by user in date range
     */
    @Query("SELECT s FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate " +
            "ORDER BY s.startTime ASC")
    List<PomodoroSession> findByUserAndDateRange(@Param("user") User user,
                                                 @Param("startDate") Instant startDate,
                                                 @Param("endDate") Instant endDate);

    /**
     * Get completed sessions by user in date range
     */
    @Query("SELECT s FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.completed = true AND s.startTime >= :startDate AND s.startTime < :endDate " +
            "ORDER BY s.startTime ASC")
    List<PomodoroSession> findCompletedByUserAndDateRange(@Param("user") User user,
                                                          @Param("startDate") Instant startDate,
                                                          @Param("endDate") Instant endDate);

    /**
     * Get distinct dates with sessions for user
     */
    @Query("SELECT DISTINCT FUNCTION('DATE', s.startTime) FROM PomodoroSession s " +
            "WHERE s.user = :user AND s.completed = true " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate " +
            "ORDER BY FUNCTION('DATE', s.startTime) ASC")
    List<LocalDate> findDistinctSessionDates(@Param("user") User user,
                                             @Param("startDate") Instant startDate,
                                             @Param("endDate") Instant endDate);

    /**
     * Count sessions by user grouped by date
     */
    @Query("SELECT FUNCTION('DATE', s.startTime) as sessionDate, COUNT(s) as count " +
            "FROM PomodoroSession s WHERE s.user = :user AND s.completed = true " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate " +
            "GROUP BY FUNCTION('DATE', s.startTime) " +
            "ORDER BY sessionDate ASC")
    List<Object[]> countSessionsByDate(@Param("user") User user,
                                       @Param("startDate") Instant startDate,
                                       @Param("endDate") Instant endDate);

    /**
     * Sum focus minutes by user grouped by date
     */
    @Query("SELECT FUNCTION('DATE', s.startTime) as sessionDate, " +
            "SUM(s.durationMinutes) as totalMinutes " +
            "FROM PomodoroSession s WHERE s.user = :user AND s.completed = true " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate " +
            "GROUP BY FUNCTION('DATE', s.startTime) " +
            "ORDER BY sessionDate ASC")
    List<Object[]> sumFocusMinutesByDate(@Param("user") User user,
                                         @Param("startDate") Instant startDate,
                                         @Param("endDate") Instant endDate);

    /**
     * Get most productive day (max focus minutes in a single day)
     */
    @Query("SELECT FUNCTION('DATE', s.startTime) as sessionDate, " +
            "SUM(s.durationMinutes) as totalMinutes " +
            "FROM PomodoroSession s WHERE s.user = :user AND s.completed = true " +
            "GROUP BY FUNCTION('DATE', s.startTime) " +
            "ORDER BY totalMinutes DESC")
    List<Object[]> findMostProductiveDay(@Param("user") User user);

    /**
     * Count morning sessions (before 12pm)
     */
    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.completed = true AND EXTRACT(HOUR FROM s.startTime) < 12 " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate")
    Long countMorningSessions(@Param("user") User user,
                              @Param("startDate") Instant startDate,
                              @Param("endDate") Instant endDate);

    /**
     * Count afternoon sessions (12pm-6pm)
     */
    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.completed = true AND EXTRACT(HOUR FROM s.startTime) >= 12 " +
            "AND EXTRACT(HOUR FROM s.startTime) < 18 " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate")
    Long countAfternoonSessions(@Param("user") User user,
                                @Param("startDate") Instant startDate,
                                @Param("endDate") Instant endDate);

    /**
     * Count evening sessions (after 6pm)
     */
    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user = :user " +
            "AND s.completed = true AND EXTRACT(HOUR FROM s.startTime) >= 18 " +
            "AND s.startTime >= :startDate AND s.startTime < :endDate")
    Long countEveningSessions(@Param("user") User user,
                              @Param("startDate") Instant startDate,
                              @Param("endDate") Instant endDate);
}