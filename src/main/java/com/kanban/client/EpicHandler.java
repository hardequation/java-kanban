package com.kanban.client;

import com.kanban.controllers.TaskManager;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EpicHandler extends BaseHttpHandler {

    private final TaskManager taskManager;

    public EpicHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response;
        Integer id = getIdFromPath(exchange.getRequestURI().getPath());
        switch (exchange.getRequestMethod()) {
            case "GET":
                if (id == null) {
                    List<Epic> tasks = taskManager.getAllEpics();
                    response = HttpTaskServer.getGson().toJson(tasks);
                    sendText(exchange, response, 200);
                    return;
                }

                try {
                    Epic epic = taskManager.getEpicById(id);
                    if (subtasksInPath(exchange.getRequestURI().getPath())) {
                        response = HttpTaskServer.getGson().toJson(epic.getSubTasks());
                    } else {
                        response = HttpTaskServer.getGson().toJson(epic);
                    }
                    sendText(exchange, response, 200);
                } catch (TaskNotFoundException e) {
                    sendNotFound(exchange, e.getMessage());
                }
                break;
            case "POST":
                InputStream inputStream = exchange.getRequestBody();
                String epicString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Epic epic = HttpTaskServer.getGson().fromJson(epicString, Epic.class);
                if (epic.getId() == null) {
                    taskManager.createTask(epic);
                } else {
                    taskManager.updateTask(epic);
                }
                sendText(exchange, "", 201);
                break;
            case "DELETE":
                if (id == null) {
                    taskManager.cleanEpics();
                } else {
                    taskManager.removeEpicById(id);
                }
                sendText(exchange, "", 201);
                break;
            default:
                System.out.println("You used neither GET or POST method.");
        }
    }

    protected boolean subtasksInPath(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 && parts[3].equals("subtasks");
    }
}
