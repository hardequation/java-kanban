package com.kanban.client;

import com.google.gson.Gson;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Task;
import com.kanban.utils.TaskStatus;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskHandlerTest {
    protected TaskManager taskManager;

    Gson gson = HttpTaskServer.getGson();
    HttpExchange exchange;
    TaskHandler taskHandler;
    protected Task task1;
    protected Task task2;

    @BeforeEach
    public void setup() {
        task1 = new Task("Task 1", "Task description 1", TaskStatus.NEW);
        task2 = new Task("Task 2", "Task description 2", TaskStatus.NEW);

        taskManager = mock(TaskManager.class);
        taskHandler = new TaskHandler(taskManager);

        exchange = mock(HttpExchange.class);
    }

    @AfterEach
    public void end() {
        taskManager.cleanTasks();
    }

    @Test
    void handleGetAllTasks() throws Exception {
        List<Task> tasks = List.of(task1, task2);
        when(taskManager.getAllTasks()).thenReturn(tasks);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks"));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        taskHandler.handle(exchange);

        String expectedResponse = gson.toJson(tasks);
        verify(exchange).sendResponseHeaders(200, expectedResponse.length());

        assertEquals(expectedResponse, os.toString());

        verify(exchange).close();
    }

    @Test
    void handleGetTaskById() throws Exception {
        when(taskManager.getTaskById(1)).thenReturn(task1);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks/1"));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        taskHandler.handle(exchange);

        String expectedResponse = gson.toJson(task1);
        verify(exchange).sendResponseHeaders(200, expectedResponse.length());

        assertEquals(expectedResponse, os.toString());

        verify(exchange).close();
    }

    @Test
    void handleGetTaskByIdNotFound() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks/999"));
        doThrow(new TaskNotFoundException("Task not found")).when(taskManager).getTaskById(999);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        taskHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(404, "Task not found".length());
        assertEquals("Task not found", os.toString());

        verify(exchange).close();
    }

    @Test
    void handlePostTaskCreation() throws Exception {
        String taskJson = gson.toJson(task1);
        InputStream is = new ByteArrayInputStream(taskJson.getBytes(StandardCharsets.UTF_8));

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks"));
        when(exchange.getRequestBody()).thenReturn(is);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        taskHandler.handle(exchange);

        verify(taskManager).createTask(any(Task.class));
        verify(exchange).sendResponseHeaders(201, 0L);
        verify(exchange).close();
    }

    @Test
    void handlePostTaskUpdate() throws Exception {
        task1.setId(1);
        String taskJson = gson.toJson(task1);
        InputStream is = new ByteArrayInputStream(taskJson.getBytes(StandardCharsets.UTF_8));

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks"));
        when(exchange.getRequestBody()).thenReturn(is);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        taskHandler.handle(exchange);

        verify(taskManager).updateTask(task1);
        verify(exchange).sendResponseHeaders(201, 0L);
        verify(exchange).close();
    }

    @Test
    void handleDeleteTask() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks/1"));

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        taskHandler.handle(exchange);

        verify(taskManager).removeEpicById(1);
        verify(exchange).sendResponseHeaders(201, 0);
        verify(exchange).close();
    }

    @Test
    void handleDeleteAllTasks() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks"));

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        taskHandler.handle(exchange);

        verify(taskManager).cleanEpics();
        verify(exchange).sendResponseHeaders(201, 0);
        verify(exchange).close();
    }
}
