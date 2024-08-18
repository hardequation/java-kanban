package com.kanban.client;

import com.kanban.controllers.Managers;
import com.kanban.controllers.TaskManager;
import com.kanban.tasks.Task;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

public class UserHandler extends BaseHttpHandler {

    private final TaskManager taskManager;

    public UserHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "";
        List<Task> tasks;
        String command = getCommand(exchange.getRequestURI().getPath());

        switch (command) {
            case "history":
                tasks = taskManager.getHistory();
                response = Managers.getGson().toJson(tasks);
                break;
            case "prioritized":
                tasks = taskManager.getPrioritizedTasks();
                response = Managers.getGson().toJson(tasks);
                break;
            default:
                System.out.println("Some error appeared...");
        }

        sendText(exchange, response, 200);
    }

    protected String getCommand(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }
}
