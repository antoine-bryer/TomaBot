package com.tomabot.discord.command;

import com.tomabot.model.dto.AchievementDTO;
import com.tomabot.model.entity.User;
import com.tomabot.model.enums.AchievementRarity;
import com.tomabot.service.AchievementService;
import com.tomabot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BadgesCommand implements SlashCommand {

    private final UserService userService;
    private final AchievementService achievementService;

    @Override
    public String getName() {
        return "badges";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("badges", "View your achievement badges")
                .addOption(OptionType.STRING, "filter", "Filter badges by status", false,
                        true) // autocomplete enabled
                .addOption(OptionType.STRING, "rarity", "Filter by rarity", false,
                        true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String discordId = event.getUser().getId();
        User user = userService.findByDiscordId(discordId);

        if (user == null) {
            event.getHook().sendMessage(
                    "🏆 You don't have any badges yet! Complete your first session with `/start` to begin earning achievements!"
            ).queue();
            return;
        }

        try {
            String filter = event.getOption("filter") != null
                    ? event.getOption("filter").getAsString()
                    : "all";
            String rarityFilter = event.getOption("rarity") != null
                    ? event.getOption("rarity").getAsString()
                    : null;

            List<AchievementDTO> achievements = achievementService.getUserAchievements(user);
            AchievementService.AchievementStatsDTO stats = achievementService.getAchievementStats(user);

            // Apply filters
            achievements = applyFilters(achievements, filter, rarityFilter);

            if (achievements.isEmpty()) {
                event.getHook().sendMessage("No achievements found with those filters!").queue();
                return;
            }

            EmbedBuilder embed = buildBadgesEmbed(user, achievements, stats, filter);
            event.getHook().sendMessageEmbeds(embed.build()).queue();

            log.info("Displayed badges for user {}", discordId);

        } catch (Exception e) {
            log.error("Error retrieving badges for user {}", discordId, e);
            event.getHook().sendMessage("❌ Failed to retrieve badges. Please try again!").queue();
        }
    }

    private List<AchievementDTO> applyFilters(List<AchievementDTO> achievements,
                                              String filter, String rarityFilter) {
        List<AchievementDTO> filtered = achievements;

        // Filter by unlock status
        filtered = switch (filter) {
            case "unlocked" -> filtered.stream()
                    .filter(AchievementDTO::getUnlocked)
                    .collect(Collectors.toList());
            case "locked" -> filtered.stream()
                    .filter(a -> !a.getUnlocked())
                    .collect(Collectors.toList());
            case "close" -> filtered.stream()
                    .filter(AchievementDTO::isAlmostUnlocked)
                    .collect(Collectors.toList());
            default -> filtered;
        };

        // Filter by rarity
        if (rarityFilter != null) {
            try {
                AchievementRarity rarity = AchievementRarity.valueOf(rarityFilter.toUpperCase());
                filtered = filtered.stream()
                        .filter(a -> a.getRarity() == rarity)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid rarity filter: {}", rarityFilter);
            }
        }

        return filtered;
    }

    private EmbedBuilder buildBadgesEmbed(User user, List<AchievementDTO> achievements,
                                          AchievementService.AchievementStatsDTO stats,
                                          String filter) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.decode("#F39C12"))
                .setTitle(String.format("🏆 %s's Achievement Badges", user.getUsername()))
                .setDescription(String.format("""
                        **Progress: %d / %d** (%.1f%%)
                        
                        Filter: %s
                        """,
                        stats.unlockedAchievements(),
                        stats.totalAchievements(),
                        stats.completionPercentage(),
                        getFilterDisplay(filter)));

        // Group by rarity
        for (AchievementRarity rarity : AchievementRarity.values()) {
            List<AchievementDTO> rarityAchievements = achievements.stream()
                    .filter(a -> a.getRarity() == rarity)
                    .limit(10) // Limit per rarity
                    .toList();

            if (!rarityAchievements.isEmpty()) {
                StringBuilder fieldValue = new StringBuilder();
                for (AchievementDTO achievement : rarityAchievements) {
                    fieldValue.append(formatAchievement(achievement)).append("\n");
                }

                embed.addField(
                        rarity.getDisplayName() + " (" + rarityAchievements.size() + ")",
                        fieldValue.toString(),
                        false
                );
            }
        }

        // Show close to unlocking
        List<AchievementDTO> almostUnlocked = achievements.stream()
                .filter(AchievementDTO::isAlmostUnlocked)
                .limit(3)
                .toList();

        if (!almostUnlocked.isEmpty() && !filter.equals("unlocked")) {
            StringBuilder almostText = new StringBuilder();
            for (AchievementDTO achievement : almostUnlocked) {
                almostText.append(String.format("• %s - **%s**\n",
                        achievement.getDisplayName(),
                        achievement.getProgressDisplay()));
            }
            embed.addField("🎯 Almost There!", almostText.toString(), false);
        }

        embed.addField("💡 Tip",
                "Use `/badges filter:locked` to see what you can unlock next!\n" +
                        "Use `/badges filter:close` to see achievements you're close to unlocking!",
                false);

        embed.setFooter("Keep completing sessions and tasks to unlock more! 🍅")
                .setTimestamp(java.time.Instant.now());

        return embed;
    }

    private String formatAchievement(AchievementDTO achievement) {
        if (achievement.getUnlocked()) {
            return String.format("✅ **%s** - %s",
                    achievement.getDisplayName(),
                    achievement.getDescription());
        } else if (achievement.getIsSecret() && achievement.getProgressPercentage() < 50) {
            return String.format("🔒 **???** - %s",
                    achievement.getHint() != null ? achievement.getHint() : "Secret achievement");
        } else {
            return String.format("🔒 **%s** - %s (%s)",
                    achievement.getName(),
                    achievement.getDescription(),
                    achievement.getProgressDisplay());
        }
    }

    private String getFilterDisplay(String filter) {
        return switch (filter) {
            case "unlocked" -> "Unlocked only ✅";
            case "locked" -> "Locked only 🔒";
            case "close" -> "Almost unlocked 🎯";
            default -> "All achievements";
        };
    }
}