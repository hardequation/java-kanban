package com.kanban.client;

import com.kanban.controllers.Managers;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.PriorityTaskException;
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
        Integer id = getIdFromPath(exchange.getRequestURI().getPath());
        switch (exchange.getRequestMethod()) {
            case "GET":
                processGETRequest(exchange, id);
                break;
            case "POST":
                processPOSTRequest(exchange);
                break;
            case "DELETE":
                processDELETERequest(exchange, id);
                break;
            default:
                System.out.println("You used neither GET or POST method.");
        }
    }

    private void processGETRequest(HttpExchange exchange, Integer id) throws IOException {
        String response;
        try {
            if (id == null) {
                List<Subtask> tasks = taskManager.getAllSubtasks();
                response = Managers.getGson().toJson(tasks);
            } else {
                Subtask task = taskManager.getSubtaskById(id);
                response = Managers.getGson().toJson(task);
            }
            sendText(exchange, response, SUCCESS);
        } catch (TaskNotFoundException e) {
            sendText(exchange, e.getMessage(), NOT_FOUND);
        } catch (Exception e) {
            sendText(exchange, e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private void processPOSTRequest(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        String taskString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        Subtask task = Managers.getGson().fromJson(taskString, Subtask.class);
        try {
            if (task.getId() == null) {
                taskManager.createTask(task);
            } else {
                taskManager.updateTask(task);
            }
            sendText(exchange, "", 201);
        } catch (PriorityTaskException e) {
            sendText(exchange, "", NOT_ACCEPTABLE);
        } catch (Exception e) {
            sendText(exchange, e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }

    private void processDELETERequest(HttpExchange exchange, Integer id) throws IOException {
        try {
            if (id == null) {
                taskManager.cleanSubtasks();
            } else {
                taskManager.removeSubtaskById(id);
            }
            sendText(exchange, "", SUCCESS_NO_DATA);
        } catch (Exception e) {
            sendText(exchange, e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }
}
