package com.tomabot.discord.listener;

import com.tomabot.discord.command.SlashCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandListener extends ListenerAdapter {

    private final List<SlashCommand> commands;
    private Map<String, SlashCommand> commandMap;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (commandMap == null) {
            commandMap = commands.stream()
                    .collect(Collectors.toMap(SlashCommand::getName, Function.identity()));
        }

        String commandName = event.getName();
        SlashCommand command = commandMap.get(commandName);

        if (command != null) {
            try {
                log.info("Executing command: {} by user: {}",
                        commandName, event.getUser().getName());
                command.execute(event);
            } catch (Exception e) {
                log.error("Error executing command: {}", commandName, e);
                if (!event.isAcknowledged()) {
                    event.reply("❌ An error occurred while processing your command!")
                            .setEphemeral(true)
                            .queue();
                }
            }
        } else {
            log.warn("Unknown command: {}", commandName);
            event.reply("❌ Unknown command!")
                    .setEphemeral(true)
                    .queue();
        }
    }
}