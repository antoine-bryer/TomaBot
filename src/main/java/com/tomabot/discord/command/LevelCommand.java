package com.tomabot.discord.command;

import com.tomabot.model.dto.LevelUpDTO;
import com.tomabot.model.entity.User;
import com.tomabot.model.entity.XPTransaction;
import com.tomabot.repository.XPTransactionRepository;
import com.tomabot.service.ExperienceService;
import com.tomabot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LevelCommand implements SlashCommand {

    private final UserService userService;
    private final ExperienceService experienceService;
    private final XPTransactionRepository xpTransactionRepository;

    @Override
    public String getName() {
        return "level";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("level", "View your level and XP progress");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String discordId = event.getUser().getId();

        User user = userService.findByDiscordId(discordId);

        if (user == null) {
            event.getHook().sendMessage(
                    "⭐ You don't have a level yet! Complete your first session with `/start` to begin earning XP!"
            ).queue();
            return;
        }

        try {
            LevelUpDTO progress = experienceService.getXPProgress(user);

            if (progress.getTotalXP() == 0) {
                event.getHook().sendMessage(
                        """
                                ⭐ You haven't earned any XP yet!
                                
                                **How to earn XP:**
                                • Complete focus sessions (+25 XP per session)
                                • Complete tasks (+10 XP per task)
                                • First session of the day (+5 XP bonus)
                                • Maintain streaks (+100 XP for 7 days)
                                
                                Start with `/start` to begin your journey! 🍅"""
                ).queue();
                return;
            }

            EmbedBuilder embed = buildLevelEmbed(user, progress);
            event.getHook().sendMessageEmbeds(embed.build()).queue();

            log.info("Displayed level info for user {}", discordId);

        } catch (Exception e) {
            log.error("Error retrieving level info for user {}", discordId, e);
            event.getHook().sendMessage("❌ Failed to retrieve level info. Please try again!").queue();
        }
    }

    private EmbedBuilder buildLevelEmbed(User user, LevelUpDTO progress) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(getColorForLevel(progress.getNewLevel()))
                .setTitle(String.format("⭐ %s - Level %d", user.getUsername(), progress.getNewLevel()))
                .setThumbnail(getLevelBadgeUrl(progress.getNewLevel()));

        // Current level and XP
        embed.addField("Current Level",
                String.format("**Level %d** %s",
                        progress.getNewLevel(),
                        getLevelTier(progress.getNewLevel())),
                true);

        embed.addField("Total XP Earned",
                String.format("**%,d XP**", progress.getTotalXP()),
                true);

        embed.addField("\u200B", "\u200B", true); // Spacer

        // Progress to next level
        String progressBar = createXPProgressBar(
                progress.getXpProgressToNext(),
                progress.getXpForNextLevel()
        );

        embed.addField("Progress to Next Level",
                String.format("**Level %d** → **Level %d**\n%s\n%d / %d XP (%d%%)",
                        progress.getNewLevel(),
                        progress.getNewLevel() + 1,
                        progressBar,
                        progress.getXpProgressToNext(),
                        progress.getXpForNextLevel(),
                        progress.getProgressPercentage()),
                false);

        // Recent XP gains
        List<XPTransaction> recentTransactions = xpTransactionRepository
                .findRecentByUser(user, PageRequest.of(0, 5));

        if (!recentTransactions.isEmpty()) {
            StringBuilder recentXP = new StringBuilder();
            for (XPTransaction tx : recentTransactions) {
                String levelUpIndicator = tx.isLevelUp() ? " 🎉 **LEVEL UP!**" : "";
                recentXP.append(String.format("• **+%d XP** - %s%s\n",
                        tx.getAmount(),
                        tx.getSource().getDescription(),
                        levelUpIndicator));
            }
            embed.addField("Recent XP Gains", recentXP.toString(), false);
        }

        // XP tips
        embed.addField("💡 Earn More XP",
                """
                        • **+25 XP** per focus session
                        • **+10 XP** per completed task
                        • **+5 XP** first session each day
                        • **+100 XP** for 7-day streaks
                        • **+50 XP** per achievement unlocked""",
                false);

        // Next milestone
        int nextMilestone = getNextMilestone(progress.getNewLevel());
        if (nextMilestone > 0) {
            int xpNeeded = experienceService.calculateTotalXPToLevel(nextMilestone) - progress.getTotalXP();
            embed.addField("🎯 Next Milestone",
                    String.format("Level **%d** - %,d XP needed", nextMilestone, xpNeeded),
                    false);
        }

        embed.setFooter("Keep focusing to level up! 🍅")
                .setTimestamp(java.time.Instant.now());

        return embed;
    }

    private String createXPProgressBar(int current, int target) {
        int bars = 20;
        int filled = target > 0 ? (int) ((double) current / target * bars) : 0;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "🟩" : "⬜");
        }

        return sb.toString();
    }

    private Color getColorForLevel(int level) {
        if (level >= 100) return Color.decode("#FFD700"); // Gold
        if (level >= 75) return Color.decode("#E5E4E2");  // Platinum
        if (level >= 50) return Color.decode("#C0C0C0");  // Silver
        if (level >= 25) return Color.decode("#CD7F32");  // Bronze
        if (level >= 10) return Color.decode("#9966CC");  // Purple
        return Color.decode("#4ECDC4"); // Cyan
    }

    private String getLevelTier(int level) {
        if (level >= 100) return "👑 **LEGEND**";
        if (level >= 75) return "⚡ **ELITE**";
        if (level >= 50) return "💎 **MASTER**";
        if (level >= 25) return "🏆 **EXPERT**";
        if (level >= 10) return "⭐ **ADVANCED**";
        return "🌱 **BEGINNER**";
    }

    private String getLevelBadgeUrl(int level) {
        // Placeholder - you can add actual badge images later
        return "https://i.imgur.com/placeholder_level_" + (level / 10) + ".png";
    }

    private int getNextMilestone(int currentLevel) {
        int[] milestones = {5, 10, 15, 20, 25, 50, 75, 100};
        for (int milestone : milestones) {
            if (milestone > currentLevel) {
                return milestone;
            }
        }
        return 0; // Already at max milestone
    }
}