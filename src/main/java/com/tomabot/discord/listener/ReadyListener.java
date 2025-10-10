package com.tomabot.discord.listener;

import com.tomabot.discord.command.SlashCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadyListener extends ListenerAdapter {

    private final List<SlashCommand> commands;

    @Override
    public void onReady(ReadyEvent event) {
        log.info("🍅 TomaBot is ready!");
        log.info("Connected as: {}", event.getJDA().getSelfUser().getAsTag());
        log.info("Connected to {} guilds", event.getJDA().getGuilds().size());

        // Register slash commands globally
        registerCommands(event);
    }

    private void registerCommands(ReadyEvent event) {
        try {
            log.info("Registering {} slash commands...", commands.size());

            event.getJDA().updateCommands()
                    .addCommands(commands.stream()
                            .map(SlashCommand::getCommandData)
                            .toList())
                    .queue(
                            success -> log.info("✅ Successfully registered {} commands",
                                    commands.size()),
                            error -> log.error("❌ Failed to register commands", error)
                    );

        } catch (Exception e) {
            log.error("Error registering commands", e);
        }
    }
}