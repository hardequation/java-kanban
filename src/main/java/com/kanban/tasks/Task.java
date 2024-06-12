package com.kanban.tasks;

import com.kanban.TaskStatus;

import java.util.Objects;

public class Task {
    protected String name;
    protected String description;
    protected TaskStatus status;
    protected Integer id;

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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Integer getId() { return id; }
    public void setId(int id) { this.id = id; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
