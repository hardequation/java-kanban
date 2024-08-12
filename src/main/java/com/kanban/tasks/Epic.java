package com.kanban.tasks;

import com.kanban.utils.TaskStatus;
import com.kanban.utils.TaskType;
import com.kanban.exception.WrongTaskLogicException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Epic extends Task {

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

    public Set<Subtask> getSubTasks() {
        return subTasks;
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
        return endTime;
    }

    private void calculateStartAndEndTimesAndDuration() {
        List<Subtask> subtaskList = new ArrayList<>(
                getSubTasks().stream()
                .filter(subtask -> subtask.getStartTime() != null && subtask.getEndTime() != null)
                .toList());

        subtaskList.sort(Comparator.comparing(Task::getStartTime));

        if (!subtaskList.isEmpty()) {
            this.startTime = subtaskList.getFirst().getStartTime();
            this.endTime = subtaskList.getLast().getEndTime();

            Long duration = 0L;
            for (Subtask subtask: getSubTasks()) {
                duration += subtask.getDuration();
            }
            this.duration = Duration.ofMinutes(duration);
        }
    }
}
