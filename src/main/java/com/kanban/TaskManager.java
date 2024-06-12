package com.kanban;

import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;

import java.util.List;

public interface TaskManager {

    List<Task> getAllTasks();

    List<Task> getAllSubtasks();

    List<Task> getAllEpics();

    void cleanTasks();

    void cleanSubtasks();

    void cleanEpics();

    Task getTaskById(int id);

    Subtask getSubtaskById(int id);

    Epic getEpicById(int id);

    Integer createTask(Task task);

    Integer createTask(Subtask task);

    Integer createTask(Epic task);

    void updateTask(Task task);

    void updateTask(Subtask subtask);

    void updateTask(Epic epic);

    void removeTaskById(int id);

    void removeSubtaskById(int id);

    void removeEpicById(int id);

    List<Subtask> getSubtasks(int epicId);

    public List<Task> getHistory();
}
