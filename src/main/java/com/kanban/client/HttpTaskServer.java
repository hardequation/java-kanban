package com.kanban.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kanban.adapter.DurationAdapter;
import com.kanban.adapter.LocalDateTimeAdapter;
import com.kanban.controllers.Managers;
import com.kanban.controllers.TaskManager;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;

public class HttpTaskServer {

    private static final int PORT = 8080;
    private final HttpServer server;

    public HttpTaskServer(TaskManager taskManager) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        this.server.createContext("/tasks", new TaskHandler(taskManager));
        this.server.createContext("/subtasks", new SubtaskHandler(taskManager));
        this.server.createContext("/epics", new EpicHandler(taskManager));
        this.server.createContext("/history", new UserHandler(taskManager));
        this.server.createContext("/prioritized", new UserHandler(taskManager));
    }

    public void start() {
        this.server.start();
    }

    public void stop() {
        this.server.stop(0);
    }

    public static void main(String[] args) throws IOException {
        TaskManager manager = Managers.getDefault();
        HttpTaskServer httpServer = new HttpTaskServer(manager);
        httpServer.start();
    }


}
