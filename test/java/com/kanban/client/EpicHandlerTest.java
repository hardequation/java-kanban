package com.kanban.client;

import com.google.gson.Gson;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
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
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EpicHandlerTest {
    protected TaskManager taskManager;

    Gson gson = HttpTaskServer.getGson();
    HttpExchange exchange;
    EpicHandler epicHandler;
    protected Subtask subtask1;
    protected Subtask subtask2;
    protected Epic epic1;
    protected Epic epic2;

    @BeforeEach
    public void setup() {
        subtask1 = new Subtask("Subtask 1", "Subtask description 1", TaskStatus.NEW);
        subtask2 = new Subtask("Subtask 2", "Subtask description 2", TaskStatus.NEW);

        epic1 = new Epic("Epic 1", "Epic description 1", TaskStatus.NEW, new HashSet<>());
        epic2 = new Epic("Epic 2", "Epic description 2", TaskStatus.NEW, new HashSet<>());

        epic1.addSubtask(subtask1);
        epic1.addSubtask(subtask2);

        taskManager = mock(TaskManager.class);
        epicHandler = new EpicHandler(taskManager);

        exchange = mock(HttpExchange.class);
    }

    @AfterEach
    public void end() {
        taskManager.cleanEpics();
    }

    @Test
    void handleGetAllEpics() throws Exception {
        List<Epic> tasks = List.of(epic1, epic2);
        when(taskManager.getAllEpics()).thenReturn(tasks);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/epics"));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        String expectedResponse = gson.toJson(tasks);
        verify(exchange).sendResponseHeaders(200, expectedResponse.length());

        assertEquals(expectedResponse, os.toString());

        verify(exchange).close();
    }

    @Test
    void handleGetEpicById() throws Exception {
        when(taskManager.getEpicById(1)).thenReturn(epic1);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/epics/1"));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        String expectedResponse = gson.toJson(epic1);
        verify(exchange).sendResponseHeaders(200, expectedResponse.length());

        assertEquals(expectedResponse, os.toString());

        verify(exchange).close();
    }

    @Test
    void handleGetEpicSubtasks() throws Exception {
        List<Subtask> tasks = List.of(subtask1, subtask2);
        when(taskManager.getEpicById(1)).thenReturn(epic1);

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/epics/1/subtasks"));
        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        String expectedResponse = gson.toJson(tasks);
        verify(exchange).sendResponseHeaders(200, expectedResponse.length());

        assertEquals(expectedResponse, os.toString());

        verify(exchange).close();
    }

    @Test
    void handleGetEpicByIdNotFound() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("GET");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks/999"));
        doThrow(new TaskNotFoundException("Task not found")).when(taskManager).getEpicById(999);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        verify(exchange).sendResponseHeaders(404, "Task not found".length());
        assertEquals("Task not found", os.toString());

        verify(exchange).close();
    }

    @Test
    void handlePostEpicCreation() throws Exception {
        String taskJson = gson.toJson(epic1);
        InputStream is = new ByteArrayInputStream(taskJson.getBytes(StandardCharsets.UTF_8));

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks"));
        when(exchange.getRequestBody()).thenReturn(is);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        verify(taskManager).createTask(any(Epic.class));
        verify(exchange).sendResponseHeaders(201, 0L);
        verify(exchange).close();
    }

    @Test
    void handlePostEpicUpdate() throws Exception {
        epic1.setId(1);
        String taskJson = gson.toJson(epic1);
        InputStream is = new ByteArrayInputStream(taskJson.getBytes(StandardCharsets.UTF_8));

        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("POST");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks"));
        when(exchange.getRequestBody()).thenReturn(is);

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        verify(taskManager).updateTask(epic1);
        verify(exchange).sendResponseHeaders(201, 0L);
        verify(exchange).close();
    }

    @Test
    void handleDeleteEpic() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks/1"));

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        verify(taskManager).removeEpicById(1);
        verify(exchange).sendResponseHeaders(201, 0);
        verify(exchange).close();
    }

    @Test
    void handleDeleteAllEpics() throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(new Headers());
        when(exchange.getRequestMethod()).thenReturn("DELETE");
        when(exchange.getRequestURI()).thenReturn(new URI("/tasks"));

        OutputStream os = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(os);

        epicHandler.handle(exchange);

        verify(taskManager).cleanEpics();
        verify(exchange).sendResponseHeaders(201, 0);
        verify(exchange).close();
    }
}
