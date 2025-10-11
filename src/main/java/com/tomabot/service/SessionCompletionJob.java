package com.tomabot.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.awt.Color;

@Component
@NoArgsConstructor
@Slf4j
public class SessionCompletionJob implements Job {

    @Autowired
    private PomodoroService pomodoroService;

    @Autowired
    private FocusModeService focusModeService;

    @Autowired
    private JDA jda;

    @Override
    public void execute(JobExecutionContext context) {
        Long sessionId = context.getJobDetail().getJobDataMap().getLong("sessionId");
        String discordId = context.getJobDetail().getJobDataMap().getString("discordId");

        log.info("Executing completion job for session {}", sessionId);

        try {
            // Mark session as completed
            pomodoroService.completeSession(sessionId, discordId);

            // Disable focus mode
            focusModeService.disableFocusMode(discordId);

            // Send notification to user
            User user = jda.retrieveUserById(discordId).complete();

            if (user != null) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Color.decode("#38D39F"))
                        .setTitle("🎉 Pomodoro Complete!")
                        .setDescription("""
                                Great job! Your focus session is complete.
                                ✅ Focus mode deactivated""")
                        .addField("Time for a break?",
                                "Use `/start` to begin another session", false)
                        .setFooter("Keep up the great work! 🍅")
                        .setTimestamp(java.time.Instant.now());

                user.openPrivateChannel().queue(channel ->
                    channel.sendMessageEmbeds(embed.build()).queue(
                            success -> log.info("Sent completion notification to {}", discordId),
                            error -> log.warn("Failed to send notification to {}: {}",
                                    discordId, error.getMessage())
                    )
                );
            }

        } catch (Exception e) {
            log.error("Error executing completion job for session {}", sessionId, e);
        }
    }
}