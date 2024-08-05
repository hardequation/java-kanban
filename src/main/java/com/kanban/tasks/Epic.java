package com.kanban.tasks;

import com.kanban.TaskStatus;
import com.kanban.TaskType;
import com.kanban.WrongTaskLogicException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Epic extends Task {
    private final Set<Subtask> subTasks;

    public Epic(String name, String description, TaskStatus status, Integer id, Set<Subtask> subtasks) {
        super(name, description, status, id);
        if (subtasks.contains(id)) {
            throw new WrongTaskLogicException("ERROR: Epic can't contain subtask with id of this epic");
        }
        this.subTasks = subtasks;
    }

    public Epic(String name, String description, TaskStatus status, Set<Subtask> subtasks) {
        super(name, description, status);
        this.subTasks = subtasks;
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

    public void addSubtask(Subtask subtask) {
        if (id != null && id.equals(subtask.getId())) {
            throw new WrongTaskLogicException("ERROR: Epic can't contain subtask with id of this epic");
        }
        subTasks.add(subtask);
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
                && Objects.equals(subTasks, task.subTasks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, status, id, subTasks);
    }
}
