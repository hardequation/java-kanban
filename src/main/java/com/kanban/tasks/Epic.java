package com.kanban.tasks;

import com.kanban.TaskStatus;
import com.kanban.TaskType;
import com.kanban.WrongTaskLogicException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Epic extends Task {
    private final Set<Integer> subTaskIds;

    public Epic(String name, String description, TaskStatus status, Integer id, Set<Integer> subtaskIds) {
        super(name, description, status, id);
        if (subtaskIds.contains(id)) {
            throw new WrongTaskLogicException("ERROR: Epic can't contain subtask with id of this epic");
        }
        this.subTaskIds = subtaskIds;
    }

    public Epic(String name, String description, TaskStatus status, Set<Integer> subtaskIds) {
        super(name, description, status);
        this.subTaskIds = subtaskIds;
    }

    public Epic(String name, String description, TaskStatus status, Integer id) {
        super(name, description, status, id);
        this.subTaskIds = new HashSet<>();
    }

    public Epic(String name, String description, TaskStatus status) {
        super(name, description, status);
        this.subTaskIds = new HashSet<>();
    }

    public Set<Integer> getSubTaskIds() {
        return subTaskIds;
    }

    public void addSubtask(int subtaskId) {
        if (id != null && id.equals(subtaskId)) {
            throw new WrongTaskLogicException("ERROR: Epic can't contain subtask with id of this epic");
        }
        subTaskIds.add(subtaskId);
    }

    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Epic task = (Epic) o;
        return Objects.equals(name, task.name)
                && Objects.equals(description, task.description)
                && status == task.status
                && Objects.equals(id, task.id)
                && Objects.equals(subTaskIds, task.subTaskIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, status, id, subTaskIds);
    }
}
