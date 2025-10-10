package com.tomabot.discord.command;

import com.tomabot.model.entity.User;
import com.tomabot.service.FocusModeService;
import com.tomabot.service.PomodoroService;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class StartCommand implements SlashCommand {

    private final UserService userService;
    private final PomodoroService pomodoroService;
    private final FocusModeService focusModeService;

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("start", "Start a Pomodoro focus session")
                .addOption(OptionType.INTEGER, "duration",
                        "Duration in minutes (default: 25)", false);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String discordId = event.getUser().getId();
        String username = event.getUser().getName();

        // Get or create user
        User user = userService.getOrCreateUser(discordId, username);

        // Get duration
        Integer duration = event.getOption("duration") != null
                ? event.getOption("duration").getAsInt()
                : 25;

        // Validate duration
        if (duration < 1 || duration > 90) {
            event.getHook().sendMessage("⚠️ Duration must be between 1 and 90 minutes!").queue();
            return;
        }

        // Check if premium for custom duration
        if (duration != 25 && !user.isPremiumActive()) {
            event.getHook().sendMessage(
                    "⚠️ Custom durations are a premium feature! Using default 25 minutes.\n" +
                            "Upgrade to premium with `/premium` to unlock custom durations!"
            ).queue();
            duration = 25;
        }

        // Start session
        try {
            pomodoroService.startSession(user, duration);

            // Enable focus mode (role + mute)
            focusModeService.enableFocusMode(discordId);

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode("#FF6B6B"))
                    .setTitle("🍅 Pomodoro Started!")
                    .setDescription("Time to focus! Your session has begun.\n\n" +
                            "🔴 **Focus mode activated**\n" +
                            "• Role '🍅 In Focus' added\n" +
                            "• Voice muted (if connected)")
                    .addField("Duration", duration + " minutes", true)
                    .addField("Ends at", String.format("<t:%d:t>",
                            System.currentTimeMillis() / 1000 + (duration * 60)), true)
                    .setFooter("Use /status to check your progress • /stop to end early")
                    .setTimestamp(java.time.Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();

            log.info("User {} started a {} minute session", discordId, duration);

        } catch (IllegalStateException e) {
            event.getHook().sendMessage("⚠️ " + e.getMessage()).queue();
        } catch (Exception e) {
            log.error("Error starting session for user {}", discordId, e);
            event.getHook().sendMessage("❌ An error occurred. Please try again!").queue();
        }
    }
}