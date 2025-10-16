package com.tomabot.model.entity;

import com.tomabot.model.enums.AchievementRarity;
import com.tomabot.model.enums.AchievementType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Achievement entity with enhanced properties
 */
@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 10)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false, length = 50)
    private AchievementType requirementType;

    @Column(name = "requirement_value", nullable = false)
    private Integer requirementValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AchievementRarity rarity = AchievementRarity.COMMON;

    @Column(name = "xp_reward")
    @Builder.Default
    private Integer xpReward = 50;

    @Column(name = "is_secret")
    @Builder.Default
    private Boolean isSecret = false;

    @Column(length = 200)
    private String hint;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Get total XP reward (base + rarity bonus)
     */
    public int getTotalXPReward() {
        return xpReward + rarity.getXpBonus();
    }
}