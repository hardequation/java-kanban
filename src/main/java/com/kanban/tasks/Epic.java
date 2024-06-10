package com.kanban.tasks;

import com.kanban.TaskStatus;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Integer> subTaskIds;

    public Epic(String name, String description, TaskStatus status, Integer id, List<Integer> subtaskIds) throws Exception {
        super(name, description, status, id);
        if (subtaskIds.contains(id)) {
            throw new Exception("ERROR: Epic can't contain subtask with id of this epic");
        }
        this.subTaskIds = subtaskIds;
    }

    public Epic(String name, String description, TaskStatus status, List<Integer> subtaskIds) {
        super(name, description, status);
        this.subTaskIds = subtaskIds;
    }

    public Epic(String name, String description, TaskStatus status, Integer id) {
        super(name, description, status, id);
        this.subTaskIds = new ArrayList<>();
    }

    public Epic(String name, String description, TaskStatus status) {
        super(name, description, status);
        this.subTaskIds = new ArrayList<>();
    }

    public List<Integer> getSubTaskIds() {
        return subTaskIds;
    }

    public void addSubtask(Subtask subtask) {
        subTaskIds.add(subtask.getId());
    }

}
