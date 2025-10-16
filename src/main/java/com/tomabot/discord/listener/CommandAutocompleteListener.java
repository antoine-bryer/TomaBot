package com.tomabot.discord.listener;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

/**
 * Listener for command autocomplete interactions
 */
@Component
public class CommandAutocompleteListener extends ListenerAdapter {

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        String commandName = event.getName();
        String focusedOption = event.getFocusedOption().getName();
        String userInput = event.getFocusedOption().getValue().toLowerCase();

        switch (commandName) {
            case "stats" -> handleStatsAutocomplete(event, focusedOption, userInput);
            case "badges" -> handleBadgesAutocomplete(event, focusedOption, userInput);
            case "leaderboard" -> handleLeaderboardAutocomplete(event, focusedOption, userInput);
            default -> event.replyChoices(List.of()).queue();
        }
    }

    /**
     * Autocomplete for /stats command
     */
    private void handleStatsAutocomplete(CommandAutoCompleteInteractionEvent event,
                                         String focusedOption, String userInput) {
        if ("period".equals(focusedOption)) {
            List<Command.Choice> choices = Stream.of(
                            new Command.Choice("📅 Today", "today"),
                            new Command.Choice("📊 This Week", "week"),
                            new Command.Choice("📈 This Month", "month"),
                            new Command.Choice("🌟 All Time", "all")
                    )
                    .filter(choice -> choice.getName().toLowerCase().contains(userInput) ||
                            choice.getAsString().toLowerCase().contains(userInput))
                    .limit(25)
                    .toList();

            event.replyChoices(choices).queue();
        }
    }

    /**
     * Autocomplete for /badges command
     */
    private void handleBadgesAutocomplete(CommandAutoCompleteInteractionEvent event,
                                          String focusedOption, String userInput) {
        if ("filter".equals(focusedOption)) {
            List<Command.Choice> choices = Stream.of(
                            new Command.Choice("📋 All Badges", "all"),
                            new Command.Choice("✅ Unlocked Only", "unlocked"),
                            new Command.Choice("🔒 Locked Only", "locked"),
                            new Command.Choice("🎯 Almost Unlocked", "close")
                    )
                    .filter(choice -> choice.getName().toLowerCase().contains(userInput) ||
                            choice.getAsString().toLowerCase().contains(userInput))
                    .limit(25)
                    .toList();

            event.replyChoices(choices).queue();
        } else if ("rarity".equals(focusedOption)) {
            List<Command.Choice> choices = Stream.of(
                            new Command.Choice("⚪ Common", "common"),
                            new Command.Choice("🟢 Uncommon", "uncommon"),
                            new Command.Choice("🔵 Rare", "rare"),
                            new Command.Choice("🟣 Epic", "epic"),
                            new Command.Choice("🟡 Legendary", "legendary"),
                            new Command.Choice("🔴 Mythic", "mythic")
                    )
                    .filter(choice -> choice.getName().toLowerCase().contains(userInput) ||
                            choice.getAsString().toLowerCase().contains(userInput))
                    .limit(25)
                    .toList();

            event.replyChoices(choices).queue();
        }
    }

    /**
     * Autocomplete for /leaderboard command
     */
    private void handleLeaderboardAutocomplete(CommandAutoCompleteInteractionEvent event,
                                               String focusedOption, String userInput) {
        if ("type".equals(focusedOption)) {
            List<Command.Choice> choices = Stream.of(
                            new Command.Choice("👑 Level", "level"),
                            new Command.Choice("⭐ Total XP", "xp"),
                            new Command.Choice("🍅 Sessions", "sessions"),
                            new Command.Choice("⏱️ Focus Time", "focus_time"),
                            new Command.Choice("🔥 Streak", "streak"),
                            new Command.Choice("✅ Tasks", "tasks"),
                            new Command.Choice("🏆 Achievements", "achievements")
                    )
                    .filter(choice -> choice.getName().toLowerCase().contains(userInput) ||
                            choice.getAsString().toLowerCase().contains(userInput))
                    .limit(25)
                    .toList();

            event.replyChoices(choices).queue();
        } else if ("scope".equals(focusedOption)) {
            List<Command.Choice> choices = Stream.of(
                            new Command.Choice("🌍 Global", "global"),
                            new Command.Choice("🏠 Server", "server")
                    )
                    .filter(choice -> choice.getName().toLowerCase().contains(userInput) ||
                            choice.getAsString().toLowerCase().contains(userInput))
                    .limit(25)
                    .toList();

            event.replyChoices(choices).queue();
        }
    }
}