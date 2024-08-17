package com.kanban.client;

import com.google.gson.Gson;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Subtask;
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

class SubtaskHandlerTest {
    protected TaskManager taskManager;
    Gson gson = HttpTaskServer.getGson();
    HttpExchange exchange;
    SubtaskHandler subtaskHandler;
    protected Subtask subtask1;
    protected Subtask subtask2;

    @BeforeEach
    public void setup() {
        subtask1 = new Subtask("Subtask 1", "Subtask description 1", TaskStatus.NEW);
        subtask2 = new Subtask("Subtask 2", "Subtask description 2", TaskStatus.NEW);
        
        taskManager = mock(TaskManager.class);
        subtaskHandler = new SubtaskHandler(taskManager);

        exchange = mock(HttpExchange.class);
    }

    @AfterEach
    public void end() {
        taskManager.cleanSubtasks();
    }

    @Test
    void handleGetAllSubtasks() throws Exception {
        List<Subtask> tasks = List.of(subtask1, subtask2);
        when(taskManager.getAllSubtasks()).thenReturn(tasks);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/subtasks"));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        subtaskHandler.handle(exchange);

        String expectedResponse = gson.toJson(tasks);
        verify(exchange).sendResponseHeaders(200, expectedResponse.length());

        assertEquals(expectedResponse, os.toString());

        verify(exchange).close();
    }

    @Test
    void handleGetSubtaskById() throws Exception {
        when(taskManager.getSubtaskById(1)).thenReturn(subtask1);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/subtasks/1"));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        subtaskHandler.handle(exchange);

        String expectedResponse = gson.toJson(subtask1);
        verify(exchange).sendResponseHeaders(200, expectedResponse.length());

        assertEquals(expectedResponse, os.toString());

        verify(exchange).close();
    }

    @Test
    void handleGetSubtaskByIdNotFound() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/subtasks/999"));
        doThrow(new TaskNotFoundException("Task not found")).when(taskManager).getSubtaskById(999);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        subtaskHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(404, "Task not found".length());
        assertEquals("Task not found", os.toString());

        verify(exchange).close();
    }

    @Test
    void handlePostSubtaskCreation() throws Exception {
        String taskJson = gson.toJson(subtask1);
        InputStream is = new ByteArrayInputStream(taskJson.getBytes(StandardCharsets.UTF_8));

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(new URI("/subtasks"));
        when(exchange.getRequestBody()).thenReturn(is);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        subtaskHandler.handle(exchange);

        verify(taskManager).createTask(any(Subtask.class));
        verify(exchange).sendResponseHeaders(201, 0L);
        verify(exchange).close();
    }

    @Test
    void handlePostSubtaskUpdate() throws Exception {
        subtask1.setId(1);
        String taskJson = gson.toJson(subtask1);
        InputStream is = new ByteArrayInputStream(taskJson.getBytes(StandardCharsets.UTF_8));

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(new URI("/subtasks"));
        when(exchange.getRequestBody()).thenReturn(is);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        subtaskHandler.handle(exchange);

        verify(taskManager).updateTask(subtask1);
        verify(exchange).sendResponseHeaders(201, 0L);
        verify(exchange).close();
    }

    @Test
    void handleDeleteSubtask() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(exchange.getRequestURI()).thenReturn(new URI("/subtasks/1"));

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        subtaskHandler.handle(exchange);

        verify(taskManager).removeEpicById(1);
        verify(exchange).sendResponseHeaders(201, 0);
        verify(exchange).close();
    }

    @Test
    void handleDeleteAllSubtasks() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(exchange.getRequestURI()).thenReturn(new URI("/subtasks"));

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        subtaskHandler.handle(exchange);

        verify(taskManager).cleanEpics();
        verify(exchange).sendResponseHeaders(201, 0);
        verify(exchange).close();
    }
}
