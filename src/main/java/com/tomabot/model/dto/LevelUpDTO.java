package com.tomabot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for level-up events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpDTO {

    private String discordId;
    private String username;

    private Integer previousLevel;
    private Integer newLevel;

    private Integer totalXP;
    private Integer xpGained;

    private Integer xpForNextLevel;
    private Integer xpProgressToNext;

    private String rewardMessage;
    private Boolean hasNewBadge;

    /**
     * Calculate XP progress percentage to next level
     */
    public int getProgressPercentage() {
        if (xpForNextLevel == 0) return 100;
        return (int) ((double) xpProgressToNext / xpForNextLevel * 100);
    }

    /**
     * Get number of levels gained
     */
    public int getLevelsGained() {
        return newLevel - previousLevel;
    }

    /**
     * Check if multiple levels were gained at once
     */
    public boolean isMultipleLevels() {
        return getLevelsGained() > 1;
    }
}