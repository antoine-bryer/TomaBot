package com.tomabot.repository;

import com.tomabot.model.entity.Achievement;
import com.tomabot.model.enums.AchievementRarity;
import com.tomabot.model.enums.AchievementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    /**
     * Find achievement by code
     */
    Optional<Achievement> findByCode(String code);

    /**
     * Find all enabled achievements
     */
    List<Achievement> findByIsEnabledTrueOrderByDisplayOrderAsc();

    /**
     * Find achievements by type
     */
    List<Achievement> findByRequirementTypeAndIsEnabledTrue(AchievementType type);

    /**
     * Find achievements by rarity
     */
    List<Achievement> findByRarityAndIsEnabledTrueOrderByDisplayOrderAsc(AchievementRarity rarity);

    /**
     * Find secret achievements
     */
    List<Achievement> findByIsSecretTrueAndIsEnabledTrue();

    /**
     * Find non-secret achievements
     */
    List<Achievement> findByIsSecretFalseAndIsEnabledTrueOrderByDisplayOrderAsc();

    /**
     * Count total achievements
     */
    @Query("SELECT COUNT(a) FROM Achievement a WHERE a.isEnabled = true")
    Long countEnabled();

    /**
     * Count achievements by rarity
     */
    @Query("SELECT COUNT(a) FROM Achievement a WHERE a.rarity = :rarity AND a.isEnabled = true")
    Long countByRarity(@Param("rarity") AchievementRarity rarity);
}