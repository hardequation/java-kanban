package com.kanban;

import com.kanban.tasks.Task;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private final Map<Integer, TaskNode> history;

    private TaskNode firstTask;
    private TaskNode lastTask;

    public InMemoryHistoryManager() {
        this.history = new HashMap<>();
    }

    @Override
    public void add(Task task) {
        int id = task.getId();

        if (history.isEmpty()) {
            TaskNode taskNode = new TaskNode(task);
            firstTask = taskNode;
            lastTask = taskNode;
            history.put(id, taskNode);
            return;
        }

        if (history.containsKey(id)) {
            remove(id);
        }

        TaskNode newLastNode = linkLast(task);
        history.put(id, newLastNode);
    }

    @Override
    public List<Task> getHistory() {
        List<Task> historyList = new ArrayList<>(history.size());
        TaskNode node = firstTask;
        while (node != null) {
            historyList.add(node.getTask());
            node = node.next;
        }
        return historyList;
    }

    @Override
    public void remove(int id) {
        TaskNode node = history.remove(id);

        if (node != null) {
            removeNode(node);
        }
    }

    private void removeNode(TaskNode node) {
        if (node.prev == null && node.next == null) {
            return;
        }

        if (node.prev == null) {
            node.next.prev = null;
        } else if (node.next == null) {
            node.prev.next = null;
        } else {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
    }

    private TaskNode linkLast(Task task) {
        TaskNode newNode = new TaskNode(task);

        if (lastTask != null) {
            newNode.prev = lastTask;
            lastTask.next = newNode;
        }
        lastTask = newNode;
        return newNode;
    }

    private static class TaskNode {

        Task value;

        TaskNode prev;

        TaskNode next;

        public TaskNode(Task task) {
            this.value = task;
            this.prev = null;
            this.next = null;
        }

        public Task getTask() {
            return value;
        }
    }
}
