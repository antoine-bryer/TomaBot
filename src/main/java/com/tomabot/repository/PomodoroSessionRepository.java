package com.tomabot.repository;

import com.tomabot.model.entity.PomodoroSession;
import com.tomabot.model.entity.User;
import com.tomabot.model.enums.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface PomodoroSessionRepository extends JpaRepository<PomodoroSession, Long> {

    List<PomodoroSession> findByUserOrderByStartTimeDesc(User user);

    List<PomodoroSession> findByUserAndStartTimeBetween(User user, Instant start, Instant end);

    @Query("SELECT COUNT(s) FROM PomodoroSession s WHERE s.user = :user AND s.completed = true")
    Long countCompletedSessionsByUser(@Param("user") User user);

    @Query("SELECT SUM(s.durationMinutes) FROM PomodoroSession s " +
            "WHERE s.user = :user AND s.completed = true AND s.sessionType = :type")
    Long sumDurationByUserAndType(@Param("user") User user, @Param("type") SessionType type);

    @Query("SELECT s FROM PomodoroSession s WHERE s.user = :user AND s.startTime >= :since " +
            "ORDER BY s.startTime DESC")
    List<PomodoroSession> findRecentSessions(@Param("user") User user, @Param("since") Instant since);

}