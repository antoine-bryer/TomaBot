package com.tomabot.discord.command;

import com.tomabot.model.entity.User;
import com.tomabot.service.StatsService;
import com.tomabot.service.UserService;
import com.tomabot.model.dto.UserStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatsCommand implements SlashCommand {

    private final UserService userService;
    private final StatsService statsService;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("stats", "View your productivity statistics")
                .addOptions(
                        new OptionData(OptionType.STRING, "period", "Time period for stats")
                                .addChoice("Today", "today")
                                .addChoice("This Week", "week")
                                .addChoice("This Month", "month")
                                .addChoice("All Time", "all")
                                .setRequired(false)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String discordId = event.getUser().getId();
        User user = userService.findByDiscordId(discordId);

        if (user == null) {
            event.getHook().sendMessage(
                    "📊 You don't have any stats yet! Start your first session with `/start`"
            ).queue();
            return;
        }

        // Get period (default: today)
        String period = event.getOption("period") != null
                ? event.getOption("period").getAsString()
                : "today";

        try {
            UserStatsDTO stats = statsService.getUserStats(user, period);

            // Check if user has any stats at all
            if (stats.getTotalFocusMinutes() == 0 && stats.getSessionsCompleted() == 0) {
                event.getHook().sendMessage(
                        "📊 You don't have any stats for this period yet!\n" +
                                "Complete your first session with `/start` to start tracking your progress! 🍅"
                ).queue();
                return;
            }

            EmbedBuilder embed = buildStatsEmbed(user, stats, period, event.getUser().getEffectiveAvatarUrl());
            event.getHook().sendMessageEmbeds(embed.build()).queue();

            log.info("Displayed {} stats for user {}", period, discordId);

        } catch (Exception e) {
            log.error("Error retrieving stats for user {}", discordId, e);
            event.getHook().sendMessage("❌ Failed to retrieve stats. Please try again!").queue();
        }
    }

    private EmbedBuilder buildStatsEmbed(User user, UserStatsDTO stats, String period, String avatarUrl) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(getColorForPeriod(period))
                .setTitle(String.format("📊 %s - %s", user.getUsername().toUpperCase(), getPeriodTitle(period)))
                .setThumbnail(avatarUrl);

        // Main stats
        embed.addField("🍅 Focus Time",
                formatDuration(stats.getTotalFocusMinutes()), true);
        embed.addField("✅ Sessions Completed",
                String.format("%d / %d", stats.getSessionsCompleted(), stats.getSessionsTotal()), true);
        embed.addField("📈 Completion Rate",
                String.format("%d%%", stats.getCompletionRate()), true);

        // Streak & Productivity
        if (!period.equals("today")) {
            embed.addField("🔥 Current Streak",
                    stats.getCurrentStreak() + " days", true);
            embed.addField("⭐ Best Streak",
                    stats.getBestStreak() + " days", true);
        }

        embed.addField("✅ Tasks Completed",
                String.valueOf(stats.getTasksCompleted()), true);

        // Progress bar
        if (stats.getSessionsCompleted() > 0) {
            String progressBar = createProgressBar(
                    stats.getSessionsCompleted(),
                    stats.getSessionsTotal()
            );
            embed.addField("Session Progress", progressBar, false);
        }

        // Daily breakdown (for week/month)
        if (period.equals("week") || period.equals("month")) {
            String chart = createMiniChart(stats.getDailyBreakdown());
            embed.addField("📊 Daily Activity", "```\n" + chart + "\n```", false);
        }

        // Trend indicator
        String trend = getTrendIndicator(stats.getTrendPercentage());
        embed.addField("📈 Trend",
                period.equals("today") ?
                        String.format("%s %+.1f%% vs yesterday", trend, stats.getTrendPercentage()) :
                        String.format("%s %+.1f%% vs previous %s", trend, stats.getTrendPercentage(), period),
                false);

        // Level & XP (Phase 2)
        if (stats.getLevel() != null) {
            embed.addField("⭐ Level",
                    String.format("Level %d (%d XP)", stats.getLevel(), stats.getCurrentXP()),
                    true);
            int xpToNext = stats.getXpToNextLevel() - stats.getCurrentXP();
            embed.addField("📊 Next Level",
                    String.format("%d XP needed", xpToNext),
                    true);
        }

        // Footer with tips
        embed.setFooter(getMotivationalFooter(stats.getCompletionRate()))
                .setTimestamp(java.time.Instant.now());

        return embed;
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + " min";
        }
        int hours = minutes / 60;
        int remainingMins = minutes % 60;
        return String.format("%dh %02dm", hours, remainingMins);
    }

    private String createProgressBar(int completed, int total) {
        if (total == 0) return "⬜⬜⬜⬜⬜⬜⬜⬜⬜⬜ 0%";

        int percentage = (int) ((double) completed / total * 100);
        int filled = percentage / 10;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "🟩" : "⬜");
        }
        bar.append(" ").append(percentage).append("%");

        return bar.toString();
    }

    private String createMiniChart(int[] dailyData) {
        if (dailyData == null || dailyData.length == 0) {
            return "No data available";
        }

        // Find max value for scaling
        int max = 0;
        for (int value : dailyData) {
            if (value > max) max = value;
        }

        if (max == 0) return "No sessions yet";

        // Create vertical bar chart (5 rows)
        StringBuilder chart = new StringBuilder();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        // Draw bars
        for (int row = 4; row >= 0; row--) {
            for (int day = 0; day < Math.min(dailyData.length, 7); day++) {
                int barHeight = (int) ((double) dailyData[day] / max * 5);
                chart.append(barHeight > row ? "█" : " ").append(" ");
            }
            chart.append("\n");
        }

        // Draw labels
        for (int i = 0; i < Math.min(dailyData.length, 7); i++) {
            chart.append(days[i].charAt(0)).append(" ");
        }

        return chart.toString();
    }

    private String getTrendIndicator(double trendPercentage) {
        if (trendPercentage > 10) return "🚀";
        if (trendPercentage > 0) return "↗️";
        if (trendPercentage == 0) return "➡️";
        if (trendPercentage > -10) return "↘️";
        return "📉";
    }

    private String getMotivationalFooter(int completionRate) {
        if (completionRate >= 90) {
            return "🌟 Outstanding! You're a productivity master!";
        } else if (completionRate >= 75) {
            return "🔥 Excellent work! Keep up the momentum!";
        } else if (completionRate >= 50) {
            return "💪 Good progress! You're building great habits!";
        } else if (completionRate > 0) {
            return "🌱 Every session counts! Keep growing!";
        } else {
            return "🍅 Ready to start your focus journey?";
        }
    }

    private String getPeriodTitle(String period) {
        return switch (period) {
            case "today" -> "Today's Stats";
            case "week" -> "This Week's Stats";
            case "month" -> "This Month's Stats";
            case "all" -> "All-Time Stats";
            default -> "Statistics";
        };
    }

    private Color getColorForPeriod(String period) {
        return switch (period) {
            case "today" -> Color.decode("#4ECDC4");  // Cyan
            case "week" -> Color.decode("#95E1D3");   // Light green
            case "month" -> Color.decode("#38D39F");  // Green
            case "all" -> Color.decode("#FF6B6B");    // Red
            default -> Color.decode("#F8B195");       // Peach
        };
    }
}