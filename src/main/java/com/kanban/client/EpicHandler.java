package com.kanban.client;

import com.kanban.controllers.Managers;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.PriorityTaskException;
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
    protected void processGETRequest(HttpExchange exchange, Integer id) throws IOException {
        String response;
        try {
            if (id == null) {
                List<Epic> tasks = taskManager.getAllEpics();
                response = Managers.getGson().toJson(tasks);
            } else {
                Epic epic = taskManager.getEpicById(id);
                if (subtasksInPath(exchange.getRequestURI().getPath())) {
                    response = Managers.getGson().toJson(epic.getSubTasks());
                } else {
                    response = Managers.getGson().toJson(epic);
                }
            }
            sendText(exchange, response, SUCCESS);
        } catch (TaskNotFoundException e) {
            sendText(exchange, e.getMessage(), NOT_FOUND);
        } catch (Exception e) {
            sendText(exchange, e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void processPOSTRequest(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        String epicString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        Epic epic = Managers.getGson().fromJson(epicString, Epic.class);
        try {
            if (epic.getId() == null) {
                taskManager.createTask(epic);
            } else {
                taskManager.updateTask(epic);
            }
            sendText(exchange, "", SUCCESS_NO_DATA);
        } catch (PriorityTaskException e) {
            sendText(exchange, "", NOT_ACCEPTABLE);
        } catch (Exception e) {
            sendText(exchange, e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void processDELETERequest(HttpExchange exchange, Integer id) throws IOException {
        try {
            if (id == null) {
                taskManager.cleanEpics();
            } else {
                taskManager.removeEpicById(id);
            }
            sendText(exchange, "", SUCCESS_NO_DATA);
        } catch (Exception e) {
            sendText(exchange, e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    protected boolean subtasksInPath(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 && parts[3].equals("subtasks");
    }
}
