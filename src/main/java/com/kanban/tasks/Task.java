package com.kanban.tasks;

import com.kanban.TaskStatus;
import com.kanban.TaskType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
public class Task {
    protected String name;
    protected String description;
    protected TaskStatus status;
    protected Integer id;
    protected Duration duration;
    protected LocalDateTime startTime;

    public Task(String name,
                String description,
                TaskStatus status,
                Integer id,
                LocalDateTime startTime,
                Long durationMinutes) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.status = status;
        this.startTime = startTime;
        if (durationMinutes != null) {
            this.duration = Duration.ofMinutes(durationMinutes);
        }
    }

    public Task(String name, String description, TaskStatus status, Integer id) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.status = status;
    }

    public Task(String name, String description, TaskStatus status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Task(Task task) {
        this.name = task.name;
        this.description = task.description;
        this.id = task.id;
        this.status = task.status;
        this.startTime = task.startTime;
        this.duration = task.duration;
    }

    public Long getDuration() {
        if (duration != null) {
            return duration.toMinutes();
        }
        return null;
    }

    public void setDuration(Long durationMinutes) {
        this.duration = Duration.ofMinutes(durationMinutes);
    }
    public LocalDateTime getEndTime() {
        if (startTime != null && duration != null) {
            return startTime.plus(duration);
        }
        return null;
    }

    public TaskType getType() {
        return TaskType.TASK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
