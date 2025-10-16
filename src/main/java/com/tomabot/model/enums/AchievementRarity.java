package com.tomabot.model.enums;

import lombok.Getter;

/**
 * Rarity levels for achievements
 */
@Getter
public enum AchievementRarity {
    COMMON("⚪", "#95A5A6", 10),        // Easy to get
    UNCOMMON("🟢", "#2ECC71", 25),     // Moderate effort
    RARE("🔵", "#3498DB", 50),         // Significant effort
    EPIC("🟣", "#9B59B6", 100),        // Very challenging
    LEGENDARY("🟡", "#F1C40F", 200),   // Extremely rare
    MYTHIC("🔴", "#E74C3C", 500);      // Almost impossible

    private final String emoji;
    private final String color;
    private final int xpBonus;

    AchievementRarity(String emoji, String color, int xpBonus) {
        this.emoji = emoji;
        this.color = color;
        this.xpBonus = xpBonus;
    }

    public String getDisplayName() {
        return emoji + " " + name();
    }
}