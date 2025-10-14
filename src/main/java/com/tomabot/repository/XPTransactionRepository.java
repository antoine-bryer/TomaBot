package com.tomabot.repository;

import com.tomabot.model.entity.User;
import com.tomabot.model.entity.XPTransaction;
import com.tomabot.model.enums.XPSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface XPTransactionRepository extends JpaRepository<XPTransaction, Long> {

    /**
     * Find all transactions for a user
     */
    List<XPTransaction> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find recent transactions for a user
     */
    @Query("SELECT t FROM XPTransaction t WHERE t.user = :user " +
            "ORDER BY t.createdAt DESC")
    List<XPTransaction> findRecentByUser(@Param("user") User user,
                                         org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions by source
     */
    List<XPTransaction> findByUserAndSourceOrderByCreatedAtDesc(User user, XPSource source);

    /**
     * Sum total XP gained by user
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM XPTransaction t WHERE t.user = :user")
    Integer sumTotalXPByUser(@Param("user") User user);

    /**
     * Sum XP gained by user in date range
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM XPTransaction t " +
            "WHERE t.user = :user AND t.createdAt >= :startDate AND t.createdAt < :endDate")
    Integer sumXPByUserAndDateRange(@Param("user") User user,
                                    @Param("startDate") Instant startDate,
                                    @Param("endDate") Instant endDate);

    /**
     * Count level-ups for a user
     */
    @Query("SELECT COUNT(t) FROM XPTransaction t WHERE t.user = :user AND t.levelAfter > t.levelBefore")
    Long countLevelUpsByUser(@Param("user") User user);

    /**
     * Find all level-up transactions
     */
    @Query("SELECT t FROM XPTransaction t WHERE t.user = :user AND t.levelAfter > t.levelBefore " +
            "ORDER BY t.createdAt DESC")
    List<XPTransaction> findLevelUpsByUser(@Param("user") User user);

    /**
     * Check if user has gained XP today
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM XPTransaction t WHERE t.user = :user " +
            "AND t.createdAt >= :startOfDay AND t.createdAt < :endOfDay")
    Boolean hasGainedXPToday(@Param("user") User user,
                             @Param("startOfDay") Instant startOfDay,
                             @Param("endOfDay") Instant endOfDay);

    /**
     * Get XP breakdown by source
     */
    @Query("SELECT t.source, SUM(t.amount) FROM XPTransaction t " +
            "WHERE t.user = :user GROUP BY t.source")
    List<Object[]> getXPBreakdownBySource(@Param("user") User user);
}