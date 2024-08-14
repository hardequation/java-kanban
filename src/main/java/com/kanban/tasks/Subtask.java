package com.kanban.tasks;

import com.kanban.utils.TaskStatus;
import com.kanban.utils.TaskType;
import com.kanban.exception.WrongTaskLogicException;

import java.time.LocalDateTime;

public class Subtask extends Task {

    private Integer epicId;

    public Subtask(String name,
                   String description,
                   TaskStatus status,
                   Integer id,
                   Integer epicId,
                   LocalDateTime startTime,
                   Long durationMinutes) {
        super(name, description, status, id, startTime, durationMinutes);
        if (epicId != null && epicId.equals(id)) {
            throw new WrongTaskLogicException("ERROR: Subtask id can't be equal id of its epic id");
        }
        this.epicId = epicId;
    }

    public Subtask(String name, String description, TaskStatus status, Integer id, Integer epicId) {
        super(name, description, status, id);
        if (epicId != null && epicId.equals(id)) {
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

    public Integer getEpicId() {
        return epicId;
    }

    public void setEpicId(Integer epicId) {
        this.epicId = epicId;
    }

    @Override
    public TaskType getType() {
        return TaskType.SUBTASK;
    }

}
