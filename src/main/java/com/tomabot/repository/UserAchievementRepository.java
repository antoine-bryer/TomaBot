package com.tomabot.repository;

import com.tomabot.model.entity.Achievement;
import com.tomabot.model.entity.User;
import com.tomabot.model.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    /**
     * Find user achievement
     */
    Optional<UserAchievement> findByUserAndAchievement(User user, Achievement achievement);

    /**
     * Find all achievements for a user
     */
    List<UserAchievement> findByUserOrderByUnlockedAtDesc(User user);

    /**
     * Check if user has achievement
     */
    boolean existsByUserAndAchievement(User user, Achievement achievement);

    /**
     * Count unlocked achievements for user
     */
    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.user = :user")
    Long countByUser(@Param("user") User user);

    /**
     * Get recently unlocked achievements
     */
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user = :user " +
            "ORDER BY ua.unlockedAt DESC")
    List<UserAchievement> findRecentByUser(@Param("user") User user,
                                           org.springframework.data.domain.Pageable pageable);

    /**
     * Find users who unlocked a specific achievement
     */
    List<UserAchievement> findByAchievementOrderByUnlockedAtAsc(Achievement achievement);

    /**
     * Get achievement completion percentage for user
     */
    @Query("SELECT CAST(COUNT(ua) AS double) / " +
            "(SELECT COUNT(a) FROM Achievement a WHERE a.isEnabled = true) * 100 " +
            "FROM UserAchievement ua WHERE ua.user = :user")
    Double getCompletionPercentage(@Param("user") User user);
}