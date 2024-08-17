package com.kanban.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kanban.adapter.DurationAdapter;
import com.kanban.adapter.LocalDateTimeAdapter;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import com.kanban.utils.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpTaskServerTest {

    HttpTaskServer server;
    TaskManager manager;

    protected Task task1;
    protected Task task2;
    protected Subtask subtask1;
    protected Subtask subtask2;
    protected Epic epic1;
    protected Epic epic2;
    private HttpClient client;
    Gson gson;

    @BeforeEach
    public void setup() throws IOException {
        manager = mock(TaskManager.class);
        server = new HttpTaskServer(manager);
        gson = HttpTaskServer.getGson();
        client = HttpClient.newHttpClient();

        task1 = new Task("Task 1", "Task description 1", TaskStatus.NEW);
        task2 = new Task("Task 2", "Task description 2", TaskStatus.NEW);

        subtask1 = new Subtask("Subtask 1", "Subtask description 1", TaskStatus.NEW);
        subtask2 = new Subtask("Subtask 2", "Subtask description 2", TaskStatus.NEW);

        epic1 = new Epic("Epic 1", "Epic description 1", TaskStatus.NEW, new HashSet<>());
        epic2 = new Epic("Epic 2", "Epic description 2", TaskStatus.NEW, new HashSet<>());

        when(manager.getAllTasks()).thenReturn(List.of(task1, task2));
        when(manager.getTaskById(1)).thenReturn(task1);
        doThrow(new TaskNotFoundException("Task not found")).when(manager).getTaskById(999);
        when(manager.getAllSubtasks()).thenReturn(List.of(subtask1, subtask2));
        when(manager.getSubtaskById(1)).thenReturn(subtask1);
        when(manager.getAllEpics()).thenReturn(List.of(epic1, epic2));
        when(manager.getEpicById(1)).thenReturn(epic1);
        when(manager.getEpicSubtasks(1)).thenReturn(List.of(subtask1, subtask2));

        server.start();
    }

    @AfterEach
    public void end() {
        server.stop();
    }

    @Test
    void testGetGson() {
        GsonBuilder expectedBuilder = new GsonBuilder();
        expectedBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        expectedBuilder.registerTypeAdapter(Duration.class, new DurationAdapter());
        Gson expectedGson = expectedBuilder.create();

        assertEquals(expectedGson.toJson(
                LocalDateTime.of(2024, 01, 12, 9, 0, 0)),
                gson.toJson(LocalDateTime.of(2024, 01, 12, 9, 0, 0)));
        assertEquals(expectedGson.toJson(Duration.ofMinutes(10)), gson.toJson(Duration.ofMinutes(10)));
    }

    @Test
    void testGetTasks() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).getAllTasks();
        assertEquals(200, response.statusCode());
    }

    @Test
    void testGetTaskById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/1"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).getTaskById(1);
        assertEquals(200, response.statusCode());
    }

    @Test
    void testGetTaskByIdNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/999"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).getTaskById(999);
        assertEquals(404, response.statusCode());
    }

    @Test
    void testGetSubtasks() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).getAllSubtasks();
        assertEquals(200, response.statusCode());
    }

    @Test
    void testGetSubtaskById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/1"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).getSubtaskById(1);
        assertEquals(200, response.statusCode());
    }

    @Test
    void testGetEpics() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).getAllEpics();
        assertEquals(200, response.statusCode());
    }

    @Test
    void testCreateTask() throws IOException, InterruptedException {
        String taskJson = gson.toJson(task1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).createTask(any(Task.class));
        assertEquals(201, response.statusCode());
    }

    @Test
    void testCreateSubtask() throws IOException, InterruptedException {
        String taskJson = gson.toJson(subtask1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).createTask(any(Subtask.class));
        assertEquals(201, response.statusCode());
    }

    @Test
    void testCreateEpic() throws IOException, InterruptedException {
        String taskJson = gson.toJson(epic1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        verify(manager).createTask(any(Epic.class));
        assertEquals(201, response.statusCode());
    }



}
