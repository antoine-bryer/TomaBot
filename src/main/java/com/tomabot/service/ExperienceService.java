package com.tomabot.service;

import com.tomabot.model.dto.LevelUpDTO;
import com.tomabot.model.entity.User;
import com.tomabot.model.entity.UserStats;
import com.tomabot.model.entity.XPTransaction;
import com.tomabot.model.enums.XPSource;
import com.tomabot.repository.UserStatsRepository;
import com.tomabot.repository.XPTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing user experience and leveling
 */
@Service
@Slf4j
public class ExperienceService {

    private JDA jda;
    private UserStatsRepository userStatsRepository;
    private XPTransactionRepository xpTransactionRepository;

    @Autowired
    public void setJda(@Lazy JDA jda) {
        this.jda = jda;
    }

    @Autowired
    public void setUserStatsRepository(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    @Autowired
    public void setXpTransactionRepository(XPTransactionRepository xpTransactionRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
    }

    // Level progression formula: XP = level² × 50
    private static final int XP_MULTIPLIER = 50;

    /**
     * Grant XP to a user from a specific source
     */
    @Transactional
    public LevelUpDTO grantXP(User user, XPSource source, Integer amount, Long referenceId) {
        UserStats stats = userStatsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("User stats not found"));

        int xpAmount = amount != null ? amount : source.getDefaultAmount();
        int levelBefore = stats.getLevel();

        // Add XP
        stats.addXp(xpAmount);

        // Check for level-up(s)
        List<Integer> newLevels = new ArrayList<>();
        while (stats.getCurrentXp() >= calculateXPForLevel(stats.getLevel() + 1)) {
            int xpForNextLevel = calculateXPForLevel(stats.getLevel() + 1);
            stats.setCurrentXp(stats.getCurrentXp() - xpForNextLevel);
            stats.levelUp();
            newLevels.add(stats.getLevel());
            log.info("User {} leveled up to level {}", user.getDiscordId(), stats.getLevel());
        }

        userStatsRepository.save(stats);

        // Record transaction
        XPTransaction transaction = XPTransaction.builder()
                .user(user)
                .source(source)
                .amount(xpAmount)
                .levelBefore(levelBefore)
                .levelAfter(stats.getLevel())
                .referenceId(referenceId)
                .description(source.getFormattedDescription(xpAmount))
                .build();

        xpTransactionRepository.save(transaction);

        log.info("Granted {} XP to user {} from {} (Level {} -> {})",
                xpAmount, user.getDiscordId(), source, levelBefore, stats.getLevel());

        // Build result DTO
        LevelUpDTO result = LevelUpDTO.builder()
                .discordId(user.getDiscordId())
                .username(user.getUsername())
                .previousLevel(levelBefore)
                .newLevel(stats.getLevel())
                .totalXP(stats.getTotalXpEarned())
                .xpGained(xpAmount)
                .xpForNextLevel(calculateXPForLevel(stats.getLevel() + 1))
                .xpProgressToNext(stats.getCurrentXp())
                .build();

        // Add reward message if leveled up
        if (!newLevels.isEmpty()) {
            result.setRewardMessage(getLevelRewardMessage(stats.getLevel()));

            // Send level-up notification
            sendLevelUpNotification(user, result);
        }

        return result;
    }

    /**
     * Grant XP with default amount from source
     */
    @Transactional
    public void grantXP(User user, XPSource source, Long referenceId) {
        grantXP(user, source, null, referenceId); // NOSONAR
    }

    /**
     * Calculate XP required to reach a specific level
     * Formula: level² × 50
     */
    public int calculateXPForLevel(int level) {
        if (level <= 1) return 0;
        return level * level * XP_MULTIPLIER;
    }

    /**
     * Calculate total XP required from level 1 to target level
     */
    public int calculateTotalXPToLevel(int level) {
        int total = 0;
        for (int i = 2; i <= level; i++) {
            total += calculateXPForLevel(i);
        }
        return total;
    }

    /**
     * Calculate level from total XP
     */
    public int calculateLevelFromXP(int totalXP) {
        int level = 1;
        int xpAccumulated = 0;

        while (xpAccumulated + calculateXPForLevel(level + 1) <= totalXP) {
            xpAccumulated += calculateXPForLevel(level + 1);
            level++;
        }

        return level;
    }

    /**
     * Get XP progress for a user
     */
    public LevelUpDTO getXPProgress(User user) {
        UserStats stats = userStatsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("User stats not found"));

        return LevelUpDTO.builder()
                .discordId(user.getDiscordId())
                .username(user.getUsername())
                .previousLevel(stats.getLevel())
                .newLevel(stats.getLevel())
                .totalXP(stats.getTotalXpEarned())
                .xpGained(0)
                .xpForNextLevel(calculateXPForLevel(stats.getLevel() + 1))
                .xpProgressToNext(stats.getCurrentXp())
                .build();
    }

    /**
     * Check if user should get first session bonus today
     */
    @Transactional(readOnly = true)
    public boolean shouldGrantFirstSessionBonus(User user) {
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Boolean hasGainedXP = xpTransactionRepository.hasGainedXPToday(user, startOfDay, endOfDay);
        return hasGainedXP == null || !hasGainedXP;
    }

    /**
     * Get reward message for reaching a level
     */
    private String getLevelRewardMessage(int level) {
        return switch (level) {
            case 5 -> "🎉 Nice! You've reached level 5! Keep building that focus habit!";
            case 10 -> "⭐ Level 10! You're becoming a productivity master!";
            case 15 -> "🔥 Level 15! Your dedication is impressive!";
            case 20 -> "💎 Level 20! You're in the top tier of focused users!";
            case 25 -> "🏆 Level 25! Quarter-century of focus mastery!";
            case 50 -> "👑 Level 50! You're a legend! Only few reach this milestone!";
            case 75 -> "⚡ Level 75! Your focus is unmatched!";
            case 100 -> "🌟 LEVEL 100! You've achieved MAXIMUM FOCUS! 🌟";
            default -> {
                if (level % 10 == 0) {
                    yield String.format("🎊 Level %d! A new milestone achieved!", level);
                }
                yield String.format("✨ Level %d! Keep up the great work!", level);
            }
        };
    }

    /**
     * Send level-up notification to user via DM
     */
    private void sendLevelUpNotification(User user, LevelUpDTO levelUp) {
        try {
            net.dv8tion.jda.api.entities.User discordUser =
                    jda.retrieveUserById(user.getDiscordId()).complete();

            if (discordUser == null) {
                log.warn("Could not find Discord user {}", user.getDiscordId());
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(getColorForLevel(levelUp.getNewLevel()))
                    .setTitle("🎉 LEVEL UP! 🎉")
                    .setDescription(String.format("""
                                    **Congratulations %s!**
                                    
                                    "You've reached **Level %d**!""",
                            user.getUsername(),
                            levelUp.getNewLevel()))
                    .addField("Previous Level", String.valueOf(levelUp.getPreviousLevel()), true)
                    .addField("New Level", String.valueOf(levelUp.getNewLevel()), true)
                    .addField("Total XP", String.format("%,d XP", levelUp.getTotalXP()), true)
                    .addField("Progress to Next Level",
                            String.format("%d / %d XP (%d%%)",
                                    levelUp.getXpProgressToNext(),
                                    levelUp.getXpForNextLevel(),
                                    levelUp.getProgressPercentage()),
                            false);

            if (levelUp.getRewardMessage() != null) {
                embed.addField("🎁 Reward", levelUp.getRewardMessage(), false);
            }

            // Add progress bar
            String progressBar = createXPProgressBar(
                    levelUp.getXpProgressToNext(),
                    levelUp.getXpForNextLevel()
            );
            embed.addField("Next Level Progress", progressBar, false);

            embed.setFooter("Keep focusing to earn more XP! 🍅")
                    .setTimestamp(Instant.now());

            discordUser.openPrivateChannel().queue(channel ->
                channel.sendMessageEmbeds(embed.build()).queue(
                        success -> log.info("Sent level-up notification to {}", user.getDiscordId()),
                        error -> log.warn("Failed to send level-up notification: {}", error.getMessage())
                )
            );

        } catch (Exception e) {
            log.error("Error sending level-up notification to {}", user.getDiscordId(), e);
        }
    }

    /**
     * Get color based on level tier
     */
    private Color getColorForLevel(int level) {
        if (level >= 100) return Color.decode("#FFD700"); // Gold
        if (level >= 75) return Color.decode("#E5E4E2");  // Platinum
        if (level >= 50) return Color.decode("#C0C0C0");  // Silver
        if (level >= 25) return Color.decode("#CD7F32");  // Bronze
        if (level >= 10) return Color.decode("#9966CC");  // Purple
        return Color.decode("#4ECDC4"); // Cyan
    }

    /**
     * Create XP progress bar
     */
    private String createXPProgressBar(int current, int target) {
        int bars = 20;
        int filled = (int) ((double) current / target * bars);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "🟩" : "⬜");
        }

        int percentage = (int) ((double) current / target * 100);
        sb.append(String.format(" %d%%", percentage));

        return sb.toString();
    }
}