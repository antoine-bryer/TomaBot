
package com.tomabot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final Scheduler scheduler;

    public void scheduleSessionCompletion(Long sessionId, String discordId, Instant endTime) {
        try {
            JobDetail job = JobBuilder.newJob(SessionCompletionJob.class)
                    .withIdentity("session-" + sessionId, "pomodoro")
                    .usingJobData("sessionId", sessionId)
                    .usingJobData("discordId", discordId)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-session-" + sessionId, "pomodoro")
                    .startAt(Date.from(endTime))
                    .build();

            scheduler.scheduleJob(job, trigger);
            log.debug("Scheduled completion job for session {}", sessionId);

        } catch (SchedulerException e) {
            log.error("Failed to schedule session completion", e);
        }
    }

    public void cancelSessionJob(Long sessionId) {
        try {
            JobKey jobKey = JobKey.jobKey("session-" + sessionId, "pomodoro");
            scheduler.deleteJob(jobKey);
            log.debug("Cancelled job for session {}", sessionId);
        } catch (SchedulerException e) {
            log.error("Failed to cancel session job", e);
        }
    }
}