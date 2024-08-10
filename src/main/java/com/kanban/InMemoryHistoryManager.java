package com.kanban;

import com.kanban.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        remove(id);
        final TaskNode newNode = new TaskNode(lastTask, task, null);
        if (firstTask == null) {
            firstTask = newNode;
        } else {
            lastTask.next = newNode;
        }
        lastTask = newNode;
        history.put(id, newNode);
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
        TaskNode nextNode = node.next;
        TaskNode prevNode = node.prev;
        if (firstTask == node) {
            firstTask = nextNode;
        }

        if (lastTask == node) {
            lastTask = prevNode;
        }

        if (prevNode == null && nextNode == null) {
            return;
        }

        if (prevNode == null) {
            nextNode.prev = null;
        } else if (nextNode == null) {
            prevNode.next = null;
        } else {
            prevNode.next = nextNode;
            nextNode.prev = prevNode;
        }
    }

    private static class TaskNode {

        Task value;

        TaskNode prev;

        TaskNode next;

        public TaskNode(Task task) {
            this.value = task;
        }

        public TaskNode(TaskNode prev, Task task, TaskNode next) {
            this.value = task;
            this.prev = prev;
            this.next = next;
        }

        public Task getTask() {
            return value;
        }
    }
}
