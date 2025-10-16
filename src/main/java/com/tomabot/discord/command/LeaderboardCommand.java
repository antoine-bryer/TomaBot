package com.tomabot.discord.command;

import com.tomabot.model.dto.LeaderboardEntryDTO;
import com.tomabot.model.entity.User;
import com.tomabot.model.enums.LeaderboardScope;
import com.tomabot.model.enums.LeaderboardType;
import com.tomabot.service.LeaderboardService;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaderboardCommand implements SlashCommand {

    private final UserService userService;
    private final LeaderboardService leaderboardService;

    private static final int DISPLAY_LIMIT = 10;

    @Override
    public String getName() {
        return "leaderboard";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("leaderboard", "View the leaderboard rankings")
                .addOption(OptionType.STRING, "type", "Leaderboard type", false,
                        true) // autocomplete
                .addOption(OptionType.STRING, "scope", "Global or Server", false,
                        true); // autocomplete
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(false).queue(); // Public reply

        String discordId = event.getUser().getId();
        User user = userService.findByDiscordId(discordId);

        String typeStr = event.getOption("type") != null
                ? event.getOption("type").getAsString()
                : "level";
        String scopeStr = event.getOption("scope") != null
                ? event.getOption("scope").getAsString()
                : "global";

        LeaderboardType type = LeaderboardType.fromString(typeStr);
        LeaderboardScope scope = LeaderboardScope.fromString(scopeStr);

        String guildId = event.getGuild() != null ? event.getGuild().getId() : null;

        try {
            // Get top entries
            List<LeaderboardEntryDTO> topEntries = leaderboardService.getTopLeaderboard(
                    type, scope, guildId, DISPLAY_LIMIT);

            // Get user's rank if exists
            LeaderboardEntryDTO userRank = user != null
                    ? leaderboardService.getUserRank(type, scope, guildId, discordId)
                    : null;

            // Get total size
            Long totalUsers = leaderboardService.getLeaderboardSize(type, scope, guildId);

            EmbedBuilder embed = buildLeaderboardEmbed(
                    type, scope, topEntries, userRank, totalUsers);

            event.getHook().sendMessageEmbeds(embed.build()).queue();

            log.info("Displayed {} {} leaderboard", scope.getKey(), type.getKey());

        } catch (Exception e) {
            log.error("Error displaying leaderboard", e);
            event.getHook().sendMessage("❌ Failed to load leaderboard. Please try again!").queue();
        }
    }

    private EmbedBuilder buildLeaderboardEmbed(LeaderboardType type,
                                               LeaderboardScope scope,
                                               List<LeaderboardEntryDTO> entries,
                                               LeaderboardEntryDTO userRank,
                                               Long totalUsers) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(getColorForType(type))
                .setTitle(String.format("%s %s Leaderboard",
                        scope.getDisplayName(),
                        type.getFullName()))
                .setDescription(String.format("""
                        **%s**
                        
                        Total users: %,d
                        """,
                        type.getDescription(),
                        totalUsers != null ? totalUsers : 0));

        if (entries.isEmpty()) {
            embed.addField("No Data",
                    "No users have stats yet! Be the first to start focusing! 🍅",
                    false);
            return embed;
        }

        // Build leaderboard list
        StringBuilder leaderboard = new StringBuilder();
        for (LeaderboardEntryDTO entry : entries) {
            String highlight = entry.getIsCurrentUser() ? "**" : "";
            leaderboard.append(String.format("%s %s%s%s - %s\n",
                    entry.getRankDisplay(),
                    highlight,
                    entry.getUsername(),
                    highlight,
                    entry.getFormattedScore(type.getKey())));
        }

        embed.addField("🏆 Top " + DISPLAY_LIMIT, leaderboard.toString(), false);

        // Show user's rank if not in top 10
        if (userRank != null && userRank.getRank() > DISPLAY_LIMIT) {
            embed.addField("📍 Your Rank",
                    String.format("**#%d** - %s",
                            userRank.getRank(),
                            userRank.getFormattedScore(type.getKey())),
                    false);
        } else if (userRank != null) {
            embed.addField("🎉 You're in the Top " + DISPLAY_LIMIT + "!",
                    String.format("Keep up the great work! Rank **#%d**", userRank.getRank()),
                    false);
        }

        embed.addField("💡 Tips",
                "• Use `/leaderboard type:xp` for other rankings\n" +
                        "• Use `/leaderboard scope:server` for server-only rankings\n" +
                        "• Complete more sessions to climb the ranks!",
                false);

        embed.setFooter("Updated in real-time • Rankings refresh hourly")
                .setTimestamp(java.time.Instant.now());

        return embed;
    }

    private Color getColorForType(LeaderboardType type) {
        return switch (type) {
            case LEVEL -> Color.decode("#FFD700"); // Gold
            case XP -> Color.decode("#9B59B6");    // Purple
            case SESSIONS -> Color.decode("#FF6B6B"); // Red
            case FOCUS_TIME -> Color.decode("#3498DB"); // Blue
            case STREAK -> Color.decode("#E67E22");    // Orange
            case TASKS -> Color.decode("#2ECC71");     // Green
            case ACHIEVEMENTS -> Color.decode("#F1C40F"); // Yellow
        };
    }
}