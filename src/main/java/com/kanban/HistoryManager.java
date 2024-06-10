package com.kanban;

import com.kanban.tasks.Task;

import java.util.List;

interface HistoryManager {

    void add(Task task);

    List<Task> getHistory();

}
