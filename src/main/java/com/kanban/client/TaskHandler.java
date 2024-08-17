package com.kanban.client;

import com.kanban.controllers.TaskManager;
import com.kanban.exception.PriorityTaskException;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Task;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TaskHandler extends BaseHttpHandler {

    private final TaskManager taskManager;

    public TaskHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response;
        Integer id = getIdFromPath(exchange.getRequestURI().getPath());
        switch (exchange.getRequestMethod()) {
            case "GET":
                if (id == null) {
                    List<Task> tasks = taskManager.getAllTasks();
                    response = HttpTaskServer.getGson().toJson(tasks);
                } else {
                    try {
                        Task task = taskManager.getTaskById(id);
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
                Task task = HttpTaskServer.getGson().fromJson(taskString, Task.class);
                try {
                    if (task.getId() == null) {
                        taskManager.createTask(task);
                    } else {
                        taskManager.updateTask(task);
                    }
                    sendText(exchange, "", 201);
                } catch (PriorityTaskException e) {
                    sendText(exchange, "", 406);
                }
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
