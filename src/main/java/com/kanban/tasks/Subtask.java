package com.kanban.tasks;

import com.kanban.TaskStatus;
import com.kanban.WrongTaskLogicException;

public class Subtask extends Task {

    private Integer epicId;

    public Subtask(String name, String description, TaskStatus status, int id, int epicId) {
        super(name, description, status, id);
        if (epicId == id) {
            throw new WrongTaskLogicException("ERROR: Subtask id can't be equal id of its epic id");
        }
        this.epicId = epicId;
    }

    public Subtask(String name, String description, TaskStatus status, int epicId) {
        super(name, description, status);
        this.epicId = epicId;
    }

    public Subtask(String name, String description, TaskStatus status) {
        super(name, description, status);
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }
}
