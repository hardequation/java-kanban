package com.kanban.controllers;

import com.kanban.controllers.HistoryManager;
import com.kanban.controllers.InMemoryHistoryManager;
import com.kanban.controllers.InMemoryTaskManager;
import com.kanban.controllers.TaskManager;

public class Managers {
    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
