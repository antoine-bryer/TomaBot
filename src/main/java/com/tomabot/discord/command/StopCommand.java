package com.tomabot.discord.command;

import com.tomabot.model.entity.User;
import com.tomabot.service.FocusModeService;
import com.tomabot.service.PomodoroService;
import com.tomabot.service.UserService;
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
public class StopCommand implements SlashCommand {

    private final UserService userService;
    private final PomodoroService pomodoroService;
    private final FocusModeService focusModeService;

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("stop", "Stop your current Pomodoro session");
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

        try {
            pomodoroService.stopSession(user);

            // Disable focus mode
            focusModeService.disableFocusMode(discordId);

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode("#95E1D3"))
                    .setTitle("⏸️ Session Stopped")
                    .setDescription("""
                            Your Pomodoro session has been stopped.
                            ✅ Focus mode deactivated""")
                    .setFooter("Start a new session with /start")
                    .setTimestamp(java.time.Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();

            log.info("User {} stopped their session", discordId);

        } catch (IllegalStateException e) {
            event.getHook().sendMessage("⚠️ " + e.getMessage()).queue();
        }
    }
}