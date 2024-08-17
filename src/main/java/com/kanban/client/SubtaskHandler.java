package com.kanban.client;

import com.kanban.controllers.TaskManager;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Subtask;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubtaskHandler extends BaseHttpHandler {

    private final TaskManager taskManager;

    public SubtaskHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response;
        Integer id = getIdFromPath(exchange.getRequestURI().getPath());
        switch (exchange.getRequestMethod()) {
            case "GET":
                if (id == null) {
                    List<Subtask> tasks = taskManager.getAllSubtasks();
                    response = HttpTaskServer.getGson().toJson(tasks);
                } else {
                    try {
                        Subtask task = taskManager.getSubtaskById(id);
                        response = HttpTaskServer.getGson().toJson(task);
                    } catch (TaskNotFoundException e) {
                        sendNotFound(exchange, e.getMessage());
                        return;
                    }
                }
                sendText(exchange, response, 200);
                break;
            case "POST":
                InputStream inputStream = exchange.getRequestBody();
                String taskString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Subtask task = HttpTaskServer.getGson().fromJson(taskString, Subtask.class);
                if (task.getId() == null) {
                    taskManager.createTask(task);
                } else {
                    taskManager.updateTask(task);
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
}
