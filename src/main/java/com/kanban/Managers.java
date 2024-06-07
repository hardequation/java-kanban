package com.kanban;

public class Managers {
    public TaskManager getDefault() {
        return new InMemoryTaskManager();
    }
}
