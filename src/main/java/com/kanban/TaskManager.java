package com.kanban;

import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;

import java.util.List;

public interface TaskManager {

    List<Task> getAllTasks();

    List<Subtask> getAllSubtasks();

    List<Epic> getAllEpics();

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

    void removeTaskById(Integer id);

    void removeSubtaskById(Integer id);

    void removeEpicById(Integer id);

    List<Subtask> getEpicSubtasks(int epicId);

    List<Task> getHistory();

    List<Task> getPrioritizedTasks();
}
