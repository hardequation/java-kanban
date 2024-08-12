package com.kanban.controllers;

import com.kanban.tasks.Task;

import java.util.List;

public interface HistoryManager {

    void add(Task task);

    void remove(int id);

    List<Task> getHistory();

}
