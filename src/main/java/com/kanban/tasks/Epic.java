package com.kanban.tasks;

import com.kanban.TaskStatus;
import com.kanban.TaskType;
import com.kanban.WrongTaskLogicException;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Epic extends Task {

    @Getter
    private final Set<Subtask> subTasks;
    private LocalDateTime endTime;

    public Epic(String name, String description, TaskStatus status, Integer id, Set<Subtask> subtasks) {
        super(name, description, status, id);
        isCorrectSubtasksIds(subtasks, id);
        this.subTasks = subtasks;
        calculateStartAndEndTimesAndDuration();
    }

    public Epic(String name, String description, TaskStatus status, Set<Subtask> subtasks) {
        super(name, description, status);
        isCorrectSubtasksIds(subtasks, id);
        this.subTasks = subtasks;
        calculateStartAndEndTimesAndDuration();
    }

    public Epic(String name, String description, TaskStatus status, Integer id) {
        super(name, description, status, id);
        this.subTasks = new HashSet<>();
    }

    public Epic(String name, String description, TaskStatus status) {
        super(name, description, status);
        this.subTasks = new HashSet<>();
    }

    private void isCorrectSubtasksIds(Set<Subtask> subtasks, Integer epicId) {
        if (subtasks.stream()
                .map(Task::getId)
                .toList()
                .contains(epicId)) {
            throw new WrongTaskLogicException("ERROR: Epic can't contain subtask with id of this epic");
        }
    }

    public void addSubtask(Subtask subtask) {
        if (id != null && id.equals(subtask.getId())) {
            throw new WrongTaskLogicException("ERROR: Epic can't contain subtask with id of this epic");
        }
        subTasks.add(subtask);
        calculateStartAndEndTimesAndDuration();
    }

    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public LocalDateTime getEndTime() {
        if (endTime == null) {
            calculateEndTime();
        }
        return endTime;
    }

    private void calculateStartAndEndTimesAndDuration() {
        calculateStartTime();
        calculateEndTime();
        calculateDuration();
    }

    private void calculateStartTime() {
        Optional<LocalDateTime> startTime = subTasks.stream()
                .map(Task::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo);

        this.startTime = startTime.orElse(null);
    }

    private void calculateEndTime() {
        Optional<LocalDateTime> newEndTime = subTasks.stream()
                .map(Task::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo);

        this.endTime = newEndTime.orElse(null);
    }

    private void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.duration = Duration.between(startTime, endTime);
        }
    }
}
