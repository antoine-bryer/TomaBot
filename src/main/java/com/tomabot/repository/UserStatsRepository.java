package com.tomabot.repository;

import com.tomabot.model.entity.User;
import com.tomabot.model.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    /**
     * Find stats by user
     */
    Optional<UserStats> findByUser(User user);

    /**
     * Find stats by user ID
     */
    @Query("SELECT us FROM UserStats us WHERE us.user.id = :userId")
    Optional<UserStats> findByUserId(@Param("userId") Long userId);

    /**
     * Find stats by Discord ID
     */
    @Query("SELECT us FROM UserStats us WHERE us.user.discordId = :discordId")
    Optional<UserStats> findByDiscordId(@Param("discordId") String discordId);

    /**
     * Get top users by total focus minutes
     */
    @Query("SELECT us FROM UserStats us ORDER BY us.totalFocusMinutes DESC")
    List<UserStats> findTopByFocusMinutes(org.springframework.data.domain.Pageable pageable);

    /**
     * Get top users by sessions completed
     */
    @Query("SELECT us FROM UserStats us ORDER BY us.totalSessionsCompleted DESC")
    List<UserStats> findTopBySessionsCompleted(org.springframework.data.domain.Pageable pageable);

    /**
     * Get top users by current streak
     */
    @Query("SELECT us FROM UserStats us ORDER BY us.currentStreak DESC")
    List<UserStats> findTopByStreak(org.springframework.data.domain.Pageable pageable);

    /**
     * Get top users by level
     */
    @Query("SELECT us FROM UserStats us ORDER BY us.level DESC, us.currentXp DESC")
    List<UserStats> findTopByLevel(org.springframework.data.domain.Pageable pageable);

    /**
     * Count total users with stats
     */
    @Query("SELECT COUNT(us) FROM UserStats us WHERE us.totalSessionsCompleted > 0")
    Long countActiveUsers();

    /**
     * Get average focus minutes across all users
     */
    @Query("SELECT AVG(us.totalFocusMinutes) FROM UserStats us WHERE us.totalSessionsCompleted > 0")
    Double getAverageFocusMinutes();

    /**
     * Get total focus minutes across all users
     */
    @Query("SELECT SUM(us.totalFocusMinutes) FROM UserStats us")
    Long getTotalFocusMinutes();
}