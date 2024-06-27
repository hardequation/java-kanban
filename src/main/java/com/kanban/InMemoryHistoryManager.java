package com.kanban;

import com.kanban.tasks.Task;


import java.util.LinkedHashMap;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private static final int HISTORY_SIZE = 10;
    private final LinkedHashMap<Integer,Task> history;

    public InMemoryHistoryManager() {
        this.history = new LinkedHashMap<>(HISTORY_SIZE, 0.75f, true);
    }

    @Override
    public void add(Task task) {
        if (history.containsKey(task.getId())) {
            history.replace(task.getId(), task);
            return;
        }

        if (history.size() >= HISTORY_SIZE) {
            history.pollFirstEntry();
        }

        history.put(task.getId(), task);
    }

    @Override
    public List<Task> getHistory() {
        return history.values().stream().toList();
    }

    @Override
    public void remove(int id) {
        history.remove(id);
    }
}
