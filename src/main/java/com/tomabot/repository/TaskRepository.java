package com.tomabot.repository;

import com.tomabot.model.entity.Task;
import com.tomabot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserAndCompletedOrderByPositionAsc(User user, Boolean completed);

    List<Task> findByUserOrderByPositionAsc(User user);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user AND t.completed = false")
    Long countActiveTasksByUser(@Param("user") User user);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.user = :user AND t.completed = true")
    Long countCompletedTasksByUser(@Param("user") User user);

    List<Task> findByNotionIdIsNotNull();

}