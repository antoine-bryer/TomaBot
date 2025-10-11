package com.tomabot.repository;

import com.tomabot.model.entity.Task;
import com.tomabot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserOrderByPositionAsc(User user);

    List<Task> findByUserAndCompletedOrderByPositionAsc(User user, Boolean completed);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user AND t.completed = false")
    Long countActiveTasksByUser(@Param("user") User user);

    /**
     * Count all tasks by user
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user")
    Long countAllTasksByUser(@Param("user") User user);

    /**
     * Count completed tasks by user in date range
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user " +
            "AND t.completed = true AND t.completedAt >= :startDate AND t.completedAt < :endDate")
    Long countCompletedByUserAndDateRange(@Param("user") User user,
                                          @Param("startDate") Instant startDate,
                                          @Param("endDate") Instant endDate);

    /**
     * Count all completed tasks by user
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user AND t.completed = true")
    Long countCompletedTasksByUser(@Param("user") User user);

    /**
     * Get completed tasks by user in date range
     */
    @Query("SELECT t FROM Task t WHERE t.user = :user " +
            "AND t.completed = true AND t.completedAt >= :startDate AND t.completedAt < :endDate " +
            "ORDER BY t.completedAt DESC")
    List<Task> findCompletedByUserAndDateRange(@Param("user") User user,
                                               @Param("startDate") Instant startDate,
                                               @Param("endDate") Instant endDate);

    /**
     * Get tasks created in date range
     */
    @Query("SELECT t FROM Task t WHERE t.user = :user " +
            "AND t.createdAt >= :startDate AND t.createdAt < :endDate " +
            "ORDER BY t.createdAt DESC")
    List<Task> findByUserAndDateRange(@Param("user") User user,
                                      @Param("startDate") Instant startDate,
                                      @Param("endDate") Instant endDate);
}