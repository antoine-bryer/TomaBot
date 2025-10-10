package com.tomabot.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "discord_id", nullable = false, unique = true, length = 20)
    private String discordId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "is_premium")
    @Builder.Default
    private Boolean isPremium = false;

    @Column(name = "premium_expires_at")
    private Instant premiumExpiresAt;

    @Column(length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(length = 5)
    @Builder.Default
    private String language = "en";

    @Column(name = "notifications_enabled")
    @Builder.Default
    private Boolean notificationsEnabled = true;

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

    public boolean isPremiumActive() {
        if (Boolean.FALSE.equals(isPremium)) return false;
        if (premiumExpiresAt == null) return true;
        return premiumExpiresAt.isAfter(Instant.now());
    }
}