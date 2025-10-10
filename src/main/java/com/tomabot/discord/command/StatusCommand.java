package com.tomabot.discord.command;

import com.tomabot.model.entity.User;
import com.tomabot.service.PomodoroService;
import com.tomabot.service.UserService;
import com.tomabot.model.dto.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;
import java.awt.Color;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatusCommand implements SlashCommand {

    private final UserService userService;
    private final PomodoroService pomodoroService;

    @Override
    public String getName() {
        return "status";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("status", "Check your current Pomodoro session status");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String discordId = event.getUser().getId();
        User user = userService.findByDiscordId(discordId);

        if (user == null) {
            event.getHook().sendMessage("❌ You don't have an active session!").queue();
            return;
        }

        SessionStatus status = pomodoroService.getSessionStatus(user);

        if (status == null) {
            event.getHook().sendMessage("ℹ️ No active session. Start one with `/start`!").queue();
            return;
        }

        String progressBar = createProgressBar(status.getElapsedMinutes(),
                status.getTotalMinutes());

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.decode("#FF6B6B"))
                .setTitle("🍅 Session Status")
                .addField("Time Remaining",
                        String.format("%d minutes", status.getRemainingMinutes()), true)
                .addField("Elapsed",
                        String.format("%d / %d minutes",
                                status.getElapsedMinutes(),
                                status.getTotalMinutes()), true)
                .addField("Progress", progressBar, false)
                .setFooter("Use /stop to end early")
                .setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private String createProgressBar(int elapsed, int total) {
        int bars = 20;
        int filled = (int) ((double) elapsed / total * bars);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            sb.append(i < filled ? "🟥" : "⬜");
        }
        return sb.toString();
    }
}