package com.tomabot.model.dto;

import com.tomabot.model.enums.AchievementRarity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for achievement display and unlock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private AchievementRarity rarity;

    // Unlock info
    private Boolean unlocked;
    private Instant unlockedAt;
    private Integer xpAwarded;

    // Progress info
    private Integer currentProgress;
    private Integer requiredProgress;
    private Integer progressPercentage;

    // Display info
    private Boolean isSecret;
    private String hint;

    /**
     * Calculate progress percentage
     */
    public void calculateProgress() {
        if (requiredProgress != null && requiredProgress > 0) {
            this.progressPercentage = Math.min(100,
                    (int) ((double) (currentProgress != null ? currentProgress : 0) / requiredProgress * 100));
        } else {
            this.progressPercentage = Boolean.TRUE.equals(unlocked) ? 100 : 0;
        }
    }

    /**
     * Check if achievement is close to being unlocked
     */
    public boolean isAlmostUnlocked() {
        return !unlocked && progressPercentage != null && progressPercentage >= 80;
    }

    /**
     * Get display name with icon
     */
    public String getDisplayName() {
        return icon + " " + name;
    }

    /**
     * Get progress display string
     */
    public String getProgressDisplay() {
        if (Boolean.TRUE.equals(unlocked)) {
            return "✅ Unlocked";
        }
        if (Boolean.TRUE.equals(isSecret) && progressPercentage < 50) {
            return "🔒 Secret Achievement";
        }
        if (requiredProgress != null) {
            return String.format("%d / %d (%d%%)",
                    currentProgress != null ? currentProgress : 0,
                    requiredProgress,
                    progressPercentage);
        }
        return "🔒 Locked";
    }
}