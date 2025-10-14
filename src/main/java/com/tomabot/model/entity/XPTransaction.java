package com.tomabot.model.entity;

import com.tomabot.model.enums.XPSource;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * XP transaction history for tracking experience gains
 */
@Entity
@Table(name = "xp_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XPTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private XPSource source;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "level_before", nullable = false)
    private Integer levelBefore;

    @Column(name = "level_after", nullable = false)
    private Integer levelAfter;

    @Column(name = "reference_id")
    private Long referenceId; // ID de la session ou task associée

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Check if this transaction resulted in a level-up
     */
    public boolean isLevelUp() {
        return levelAfter > levelBefore;
    }
}