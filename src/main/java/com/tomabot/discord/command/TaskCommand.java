package com.tomabot.discord.command;

import com.tomabot.model.entity.Task;
import com.tomabot.model.entity.User;
import com.tomabot.service.TaskService;
import com.tomabot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.stereotype.Component;
import java.awt.Color;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskCommand implements SlashCommand {

    private final UserService userService;
    private final TaskService taskService;

    @Override
    public String getName() {
        return "task";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("task", "Manage your tasks")
                .addSubcommands(
                        new SubcommandData("add", "Add a new task")
                                .addOption(OptionType.STRING, "title", "Task title", true),
                        new SubcommandData("list", "View all your tasks"),
                        new SubcommandData("complete", "Mark a task as complete")
                                .addOption(OptionType.INTEGER, "id", "Task ID", true),
                        new SubcommandData("delete", "Delete a task")
                                .addOption(OptionType.INTEGER, "id", "Task ID", true)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String discordId = event.getUser().getId();
        String username = event.getUser().getName();
        User user = userService.getOrCreateUser(discordId, username);

        String subcommand = event.getSubcommandName();

        switch (subcommand) {
            case "add" -> handleAdd(event, user);
            case "list" -> handleList(event, user);
            case "complete" -> handleComplete(event, user);
            case "delete" -> handleDelete(event, user);
            default -> event.getHook().sendMessage("❌ Unknown subcommand").queue();
        }
    }

    private void handleAdd(SlashCommandInteractionEvent event, User user) {
        String title = event.getOption("title").getAsString();

        try {
            Task task = taskService.createTask(user, title);

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode("#38D39F"))
                    .setTitle("✅ Task Added")
                    .setDescription("**" + task.getTitle() + "**")
                    .addField("Task ID", "#" + task.getId(), true)
                    .addField("Status", "Pending", true)
                    .setFooter("View all tasks with /task list")
                    .setTimestamp(java.time.Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();

        } catch (IllegalStateException e) {
            event.getHook().sendMessage("⚠️ " + e.getMessage() +
                    "\n💎 Upgrade to premium for unlimited tasks!").queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event, User user) {
        List<Task> tasks = taskService.getUserTasks(user);

        if (tasks.isEmpty()) {
            event.getHook().sendMessage("📝 No tasks yet! Add one with `/task add`").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.decode("#4ECDC4"))
                .setTitle("📝 Your Tasks")
                .setDescription("Here are all your tasks:");

        List<Task> pending = tasks.stream().filter(t -> !t.getCompleted()).toList();
        List<Task> completed = tasks.stream().filter(Task::getCompleted).toList();

        if (!pending.isEmpty()) {
            StringBuilder pendingText = new StringBuilder();
            for (Task task : pending) {
                pendingText.append(String.format("**#%d** - %s%n", task.getId(), task.getTitle()));
            }
            embed.addField("⏳ Pending (" + pending.size() + ")", pendingText.toString(), false);
        }

        if (!completed.isEmpty()) {
            StringBuilder completedText = new StringBuilder();
            for (Task task : completed.stream().limit(5).toList()) {
                completedText.append(String.format("~~#%d - %s~~%n", task.getId(), task.getTitle()));
            }
            if (completed.size() > 5) {
                completedText.append("*...and ").append(completed.size() - 5).append(" more*");
            }
            embed.addField("✅ Completed (" + completed.size() + ")", completedText.toString(), false);
        }

        embed.setFooter("Use /task complete <id> to mark as done");
        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleComplete(SlashCommandInteractionEvent event, User user) {
        Long taskId = event.getOption("id").getAsLong();

        try {
            Task task = taskService.completeTask(user, taskId);

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode("#38D39F"))
                    .setTitle("🎉 Task Completed!")
                    .setDescription("**" + task.getTitle() + "**")
                    .setFooter("Great work! Keep it up! 🍅")
                    .setTimestamp(java.time.Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).queue();

        } catch (IllegalArgumentException e) {
            event.getHook().sendMessage("❌ Task not found or doesn't belong to you!").queue();
        }
    }

    private void handleDelete(SlashCommandInteractionEvent event, User user) {
        Long taskId = event.getOption("id").getAsLong();

        try {
            taskService.deleteTask(user, taskId);

            event.getHook().sendMessage("🗑️ Task deleted successfully!").queue();

        } catch (IllegalArgumentException e) {
            event.getHook().sendMessage("❌ Task not found or doesn't belong to you!").queue();
        }
    }
}