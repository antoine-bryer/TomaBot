package com.tomabot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * User achievement unlock record
 * Represents which achievements a user has unlocked
 */
@Entity
@Table(name = "user_achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "unlocked_at")
    @Builder.Default
    private Instant unlockedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (unlockedAt == null) {
            unlockedAt = Instant.now();
        }
    }

    /**
     * Get achievement name for easy access
     */
    public String getAchievementName() {
        return achievement != null ? achievement.getName() : "Unknown";
    }

    /**
     * Get achievement code for easy access
     */
    public String getAchievementCode() {
        return achievement != null ? achievement.getCode() : "UNKNOWN";
    }

    /**
     * Get total XP reward (base + rarity bonus)
     */
    public Integer getTotalXPReward() {
        return achievement != null ? achievement.getTotalXPReward() : 0;
    }
}