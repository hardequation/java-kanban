package com.kanban.tasks;

import com.kanban.TaskStatus;

public class Subtask extends Task {

    private final Integer epicId;

    public Subtask(String name, String description, TaskStatus status, int id, int epicId) throws Exception {
        super(name, description, status, id);
        if (epicId == id) {
            throw new Exception("ERROR: Subtask id can't be equal id of its epic id");
        }
        this.epicId = epicId;
    }

    public Subtask(String name, String description, TaskStatus status, int epicId) {
        super(name, description, status);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }
}
