package com.tomabot.service;

import com.tomabot.model.dto.AchievementDTO;
import com.tomabot.model.entity.Achievement;
import com.tomabot.model.entity.User;
import com.tomabot.model.entity.UserAchievement;
import com.tomabot.model.entity.UserStats;
import com.tomabot.model.enums.AchievementType;
import com.tomabot.model.enums.XPSource;
import com.tomabot.repository.AchievementRepository;
import com.tomabot.repository.PomodoroSessionRepository;
import com.tomabot.repository.UserAchievementRepository;
import com.tomabot.repository.UserStatsRepository;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing achievements and unlocks
 */
@Service
@Slf4j
public class AchievementService {

    private JDA jda;
    private AchievementRepository achievementRepository;
    private UserAchievementRepository userAchievementRepository;
    private UserStatsRepository userStatsRepository;
    private ExperienceService experienceService;
    private PomodoroSessionRepository pomodoroSessionRepository; // NEW

    @Autowired
    public void setJda(@Lazy JDA jda) {
        this.jda = jda;
    }

    @Autowired
    public void setAchievementRepository(AchievementRepository achievementRepository) {
        this.achievementRepository = achievementRepository;
    }

    @Autowired
    public void setUserAchievementRepository(UserAchievementRepository userAchievementRepository) {
        this.userAchievementRepository = userAchievementRepository;
    }

    @Autowired
    public void setUserStatsRepository(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    @Autowired
    public void setExperienceService(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @Autowired
    public void setPomodoroSessionRepository(PomodoroSessionRepository pomodoroSessionRepository) {
        this.pomodoroSessionRepository = pomodoroSessionRepository;
    }

    /**
     * Check and unlock achievements for user based on their stats
     */
    @Transactional
    public List<Achievement> checkAndUnlockAchievements(User user) {
        UserStats stats = userStatsRepository.findByUser(user).orElse(null);
        if (stats == null) {
            return List.of();
        }

        List<Achievement> unlocked = new ArrayList<>();
        List<Achievement> allAchievements = achievementRepository.findByIsEnabledTrueOrderByDisplayOrderAsc();

        for (Achievement achievement : allAchievements) {
            // Skip if already unlocked
            if (userAchievementRepository.existsByUserAndAchievement(user, achievement)) {
                continue;
            }

            // Check if requirement is met
            if (isRequirementMet(user, stats, achievement)) {
                unlockAchievement(user, achievement);
                unlocked.add(achievement);
            }
        }

        return unlocked;
    }

    /**
     * Check if achievement requirement is met
     */
    private boolean isRequirementMet(User user, UserStats stats, Achievement achievement) {
        return switch (achievement.getRequirementType()) {
            case SESSIONS_COMPLETED ->
                    stats.getTotalSessionsCompleted() >= achievement.getRequirementValue();
            case TOTAL_FOCUS_MINUTES ->
                    stats.getTotalFocusMinutes() >= achievement.getRequirementValue();
            case STREAK_DAYS ->
                    stats.getCurrentStreak() >= achievement.getRequirementValue();
            case TASKS_COMPLETED ->
                    stats.getTotalTasksCompleted() >= achievement.getRequirementValue();
            case LEVEL_REACHED ->
                    stats.getLevel() >= achievement.getRequirementValue();
            case MORNING_SESSIONS ->
                    checkMorningSessions(user, achievement.getRequirementValue());
            case EVENING_SESSIONS ->
                    checkEveningSessions(user, achievement.getRequirementValue());
            case SPECIAL_DATE ->
                    checkSpecialDate(achievement.getCode());
            case PERFECT_WEEK ->
                    checkPerfectWeek(user);
            default -> false;
        };
    }

    /**
     * Unlock achievement for user
     */
    @Transactional
    public void unlockAchievement(User user, Achievement achievement) {
        // Create user achievement record
        UserAchievement userAchievement = UserAchievement.builder()
                .user(user)
                .achievement(achievement)
                .build();

        userAchievementRepository.save(userAchievement);

        // Update stats
        UserStats stats = userStatsRepository.findByUser(user).orElseThrow();
        stats.setAchievementsCount(stats.getAchievementsCount() + 1);
        userStatsRepository.save(stats);

        // Grant XP reward
        experienceService.grantXP(user, XPSource.ACHIEVEMENT_UNLOCKED,
                achievement.getTotalXPReward(), achievement.getId());

        // Send notification
        sendAchievementNotification(user, achievement);

        log.info("User {} unlocked achievement: {} (+{} XP)",
                user.getDiscordId(), achievement.getName(), achievement.getTotalXPReward());
    }

    /**
     * Get all achievements with user progress
     */
    @Transactional(readOnly = true)
    public List<AchievementDTO> getUserAchievements(User user) {
        UserStats stats = userStatsRepository.findByUser(user).orElse(null);
        List<Achievement> allAchievements = achievementRepository.findByIsEnabledTrueOrderByDisplayOrderAsc();
        List<UserAchievement> unlockedAchievements = userAchievementRepository.findByUserOrderByUnlockedAtDesc(user);

        return allAchievements.stream()
                .map(achievement -> buildAchievementDTO(achievement, stats, unlockedAchievements))
                .collect(Collectors.toList());
    }

    /**
     * Build achievement DTO with progress
     */
    private AchievementDTO buildAchievementDTO(Achievement achievement, UserStats stats,
                                               List<UserAchievement> unlockedAchievements) {
        UserAchievement userAchievement = unlockedAchievements.stream()
                .filter(ua -> ua.getAchievement().getId().equals(achievement.getId()))
                .findFirst()
                .orElse(null);

        boolean unlocked = userAchievement != null;
        Integer currentProgress = stats != null ? getCurrentProgress(stats, achievement) : 0;

        AchievementDTO dto = AchievementDTO.builder()
                .id(achievement.getId())
                .code(achievement.getCode())
                .name(achievement.getName())
                .description(achievement.getDescription())
                .icon(achievement.getIcon())
                .rarity(achievement.getRarity())
                .unlocked(unlocked)
                .unlockedAt(unlocked ? userAchievement.getUnlockedAt() : null)
                .xpAwarded(unlocked ? achievement.getTotalXPReward() : null)
                .currentProgress(currentProgress)
                .requiredProgress(achievement.getRequirementValue())
                .isSecret(achievement.getIsSecret())
                .hint(achievement.getHint())
                .build();

        dto.calculateProgress();
        return dto;
    }

    /**
     * Get current progress for achievement
     */
    private Integer getCurrentProgress(UserStats stats, Achievement achievement) {
        return switch (achievement.getRequirementType()) {
            case SESSIONS_COMPLETED -> stats.getTotalSessionsCompleted();
            case TOTAL_FOCUS_MINUTES -> stats.getTotalFocusMinutes();
            case STREAK_DAYS -> stats.getCurrentStreak();
            case TASKS_COMPLETED -> stats.getTotalTasksCompleted();
            case LEVEL_REACHED -> stats.getLevel();
            default -> 0;
        };
    }

    /**
     * Check morning sessions requirement
     */
    private boolean checkMorningSessions(User user, int required) {
        try {
            // Count sessions started before 12 PM
            Instant allTimeStart = Instant.ofEpochMilli(0);
            Instant now = Instant.now();
            Long morningCount = pomodoroSessionRepository.countMorningSessions(user, allTimeStart, now);
            return morningCount != null && morningCount >= required;
        } catch (Exception e) {
            log.error("Error checking morning sessions for user {}", user.getDiscordId(), e);
            return false;
        }
    }

    /**
     * Check evening sessions requirement
     */
    private boolean checkEveningSessions(User user, int required) {
        try {
            // Count sessions started after 6 PM
            Instant allTimeStart = Instant.ofEpochMilli(0);
            Instant now = Instant.now();
            Long eveningCount = pomodoroSessionRepository.countEveningSessions(user, allTimeStart, now);
            return eveningCount != null && eveningCount >= required;
        } catch (Exception e) {
            log.error("Error checking evening sessions for user {}", user.getDiscordId(), e);
            return false;
        }
    }

    /**
     * Check special date achievements (Christmas, Halloween, etc.)
     */
    private boolean checkSpecialDate(String code) {
        LocalDate today = LocalDate.now();
        return switch (code) {
            case "CHRISTMAS_SPIRIT" -> today.getMonthValue() == 12 && today.getDayOfMonth() == 25;
            case "SPOOKY_FOCUS" -> today.getMonthValue() == 10 && today.getDayOfMonth() == 31;
            case "NEW_YEAR_FOCUS" -> today.getMonthValue() == 1 && today.getDayOfMonth() == 1;
            default -> false;
        };
    }

    /**
     * Check perfect week requirement
     */
    private boolean checkPerfectWeek(User user) {
        // Would need to check completion rate for last 7 days
        // Placeholder for now
        return false;
    }

    /**
     * Send achievement unlock notification via DM
     */
    private void sendAchievementNotification(User user, Achievement achievement) {
        try {
            net.dv8tion.jda.api.entities.User discordUser =
                    jda.retrieveUserById(user.getDiscordId()).complete();

            if (discordUser == null) {
                log.warn("Could not find Discord user {}", user.getDiscordId());
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode(achievement.getRarity().getColor()))
                    .setTitle("🏆 ACHIEVEMENT UNLOCKED! 🏆")
                    .setDescription(String.format("""
                            **%s**
                            
                            %s""",
                            achievement.getIcon() + " " + achievement.getName(),
                            achievement.getDescription()))
                    .addField("Rarity", achievement.getRarity().getDisplayName(), true)
                    .addField("XP Reward", String.format("+%d XP", achievement.getTotalXPReward()), true)
                    .setFooter("Keep it up! More achievements await! 🍅")
                    .setTimestamp(Instant.now());

            discordUser.openPrivateChannel().queue(channel ->
                    channel.sendMessageEmbeds(embed.build()).queue(
                            success -> log.info("Sent achievement notification to {}", user.getDiscordId()),
                            error -> log.warn("Failed to send achievement notification: {}", error.getMessage())
                    )
            );

        } catch (Exception e) {
            log.error("Error sending achievement notification to {}", user.getDiscordId(), e);
        }
    }

    /**
     * Get achievement statistics
     */
    @Transactional(readOnly = true)
    public AchievementStatsDTO getAchievementStats(User user) {
        Long totalAchievements = achievementRepository.countEnabled();
        Long unlockedCount = userAchievementRepository.countByUser(user);
        Double completionPercentage = userAchievementRepository.getCompletionPercentage(user);

        return new AchievementStatsDTO(
                totalAchievements != null ? totalAchievements.intValue() : 0,
                unlockedCount != null ? unlockedCount.intValue() : 0,
                completionPercentage != null ? completionPercentage : 0.0
        );
    }

    /**
     * DTO for achievement statistics
     */
    public record AchievementStatsDTO(
            int totalAchievements,
            int unlockedAchievements,
            double completionPercentage
    ) {}
}