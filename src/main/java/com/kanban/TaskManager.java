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

    Task getTaskById(int id) throws TaskNotFoundException;

    Subtask getSubtaskById(int id) throws TaskNotFoundException;

    Epic getEpicById(int id) throws TaskNotFoundException;

    void createTask(Task task);

    void createTask(Subtask task);

    void createTask(Epic task);

    void updateTask(Task task) throws TaskNotFoundException;

    void updateTask(Subtask subtask) throws TaskNotFoundException;

    void updateTask(Epic epic) throws TaskNotFoundException;

    void removeTaskById(int id);

    void removeSubtaskById(int id);

    void removeEpicById(int id);

    List<Subtask> getSubtasks(int epicId) throws TaskNotFoundException;

    public List<Task> getHistory();
}
