package com.tomabot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Aggregate user statistics entity
 * Stores cumulative stats for quick access
 */
@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Total lifetime stats
    @Column(name = "total_focus_minutes")
    @Builder.Default
    private Integer totalFocusMinutes = 0;

    @Column(name = "total_sessions_completed")
    @Builder.Default
    private Integer totalSessionsCompleted = 0;

    @Column(name = "total_sessions_interrupted")
    @Builder.Default
    private Integer totalSessionsInterrupted = 0;

    @Column(name = "total_tasks_completed")
    @Builder.Default
    private Integer totalTasksCompleted = 0;

    // Streak tracking
    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "best_streak")
    @Builder.Default
    private Integer bestStreak = 0;

    @Column(name = "last_session_date")
    private Instant lastSessionDate;

    // Level & XP (Phase 2)
    @Column(name = "level")
    @Builder.Default
    private Integer level = 1;

    @Column(name = "current_xp")
    @Builder.Default
    private Integer currentXp = 0;

    @Column(name = "total_xp_earned")
    @Builder.Default
    private Integer totalXpEarned = 0;

    // Achievements
    @Column(name = "achievements_count")
    @Builder.Default
    private Integer achievementsCount = 0;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Increment session completed count
     */
    public void incrementSessionsCompleted() {
        this.totalSessionsCompleted++;
    }

    /**
     * Increment session interrupted count
     */
    public void incrementSessionsInterrupted() {
        this.totalSessionsInterrupted++;
    }

    /**
     * Add focus minutes
     */
    public void addFocusMinutes(int minutes) {
        this.totalFocusMinutes += minutes;
    }

    /**
     * Increment tasks completed
     */
    public void incrementTasksCompleted() {
        this.totalTasksCompleted++;
    }

    /**
     * Update streak
     */
    public void updateStreak(int newStreak) {
        this.currentStreak = newStreak;
        if (newStreak > this.bestStreak) {
            this.bestStreak = newStreak;
        }
    }

    /**
     * Add XP
     */
    public void addXp(int xp) {
        this.currentXp += xp;
        this.totalXpEarned += xp;
    }

    /**
     * Level up
     */
    public void levelUp() {
        this.level++;
    }

    /**
     * Get total sessions (completed + interrupted)
     */
    public Integer getTotalSessions() {
        return totalSessionsCompleted + totalSessionsInterrupted;
    }

    /**
     * Get completion rate as percentage
     */
    public Integer getCompletionRate() {
        int total = getTotalSessions();
        if (total == 0) return 0;
        return (int) ((double) totalSessionsCompleted / total * 100);
    }
}