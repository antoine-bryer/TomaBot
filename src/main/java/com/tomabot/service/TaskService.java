package com.tomabot.service;

import com.tomabot.model.entity.Task;
import com.tomabot.model.entity.User;
import com.tomabot.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final StatsService statsService; // NEW: Stats integration

    @Value("${tomabot.limits.freemium.max-tasks:5}")
    private Integer freemiumMaxTasks;

    @Transactional
    public Task createTask(User user, String title) {
        // Check limits for freemium users
        if (!user.isPremiumActive()) {
            Long activeTasksCount = taskRepository.countActiveTasksByUser(user);
            if (activeTasksCount >= freemiumMaxTasks) {
                throw new IllegalStateException(
                        String.format("Free users can have maximum %d active tasks!", freemiumMaxTasks)
                );
            }
        }

        Task task = Task.builder()
                .user(user)
                .title(title)
                .completed(false)
                .position(getNextPosition(user))
                .build();

        task = taskRepository.save(task);
        log.info("Created task {} for user {}", task.getId(), user.getDiscordId());

        return task;
    }

    @Transactional(readOnly = true)
    public List<Task> getUserTasks(User user) {
        return taskRepository.findByUserOrderByPositionAsc(user);
    }

    @Transactional(readOnly = true)
    public List<Task> getActiveTasks(User user) {
        return taskRepository.findByUserAndCompletedOrderByPositionAsc(user, false);
    }

    @Transactional
    public Task completeTask(User user, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Task doesn't belong to you");
        }

        task.complete();
        task = taskRepository.save(task);

        // NEW: Update stats after task completion
        statsService.updateStatsAfterTaskCompletion(user);

        log.info("Completed task {} for user {}", taskId, user.getDiscordId());
        return task;
    }

    @Transactional
    public void deleteTask(User user, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Task doesn't belong to you");
        }

        taskRepository.delete(task);
        log.info("Deleted task {} for user {}", taskId, user.getDiscordId());
    }

    private Integer getNextPosition(User user) {
        List<Task> tasks = taskRepository.findByUserOrderByPositionAsc(user);
        return tasks.isEmpty() ? 0 : tasks.get(tasks.size() - 1).getPosition() + 1;
    }
}