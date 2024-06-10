package com.kanban;

import com.kanban.tasks.Task;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private static final int HISTORY_SIZE = 10;
    private final Deque<Task> history;

    public InMemoryHistoryManager() {
        this.history = new ArrayDeque<>(HISTORY_SIZE);
    }

    @Override
    public void add(Task task) {
        if (history.size() >= HISTORY_SIZE) {
            history.removeFirst();
        }
        history.add(task);
    }

    @Override
    public List<Task> getHistory() {
        return history.stream().toList();
    }
}
