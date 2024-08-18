package com.kanban.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kanban.adapter.DurationAdapter;
import com.kanban.adapter.LocalDateTimeAdapter;
import com.kanban.controllers.HistoryManager;
import com.kanban.controllers.InMemoryTaskManager;
import com.kanban.controllers.Managers;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import com.kanban.utils.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpTaskServerTest {

    HttpTaskServer server;
    TaskManager taskManager;

    HistoryManager historyManager;

    Task task1;
    Task task2;
    Subtask subtask1;
    Subtask subtask2;
    Epic epic1;
    Epic epic2;
    private HttpClient client;
    Gson gson;

    @BeforeEach
    public void setup() throws IOException {
        historyManager = Managers.getDefaultHistory();
        taskManager = new InMemoryTaskManager(historyManager);
        server = new HttpTaskServer(taskManager);
        gson = Managers.getGson();
        client = HttpClient.newHttpClient();

        task1 = new Task("Task 1", "Task description 1", TaskStatus.NEW);
        task2 = new Task("Task 2", "Task description 2", TaskStatus.NEW);

        subtask1 = new Subtask("Subtask 1", "Subtask description 1", TaskStatus.NEW);
        subtask2 = new Subtask("Subtask 2", "Subtask description 2", TaskStatus.NEW);

        epic1 = new Epic("Epic 1", "Epic description 1", TaskStatus.NEW, new HashSet<>());
        epic2 = new Epic("Epic 2", "Epic description 2", TaskStatus.NEW, new HashSet<>());
        epic1.addSubtask(subtask1);
        epic1.addSubtask(subtask2);

        taskManager.createTask(task1);
        taskManager.createTask(task2);

        taskManager.createTask(epic1);
        taskManager.createTask(epic2);

        server.start();
    }

    @AfterEach
    public void end() {
        server.stop();
    }

    @Test
    @DisplayName("test gson transformation")
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
    @DisplayName("test get all tasks method")
    void testGetTasks() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(gson.toJson(taskManager.getAllTasks()), response.body());
    }

    @Test
    @DisplayName("get task by id")
    void testGetTaskById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/1"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(gson.toJson(task1), response.body());
    }

    @Test
    @DisplayName("get task by id if task with this id doesn't exist")
    void testGetTaskByIdNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/999"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertEquals("There is no task with such ID", response.body());
    }

    @Test
    @DisplayName("create task")
    void testCreateTask() throws IOException, InterruptedException {
        String taskJson = gson.toJson(task1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("create task that intersect with existing task by execution time")
    void testCreateTaskWithIntersect() throws IOException, InterruptedException {
        task1.setId(null); // set in null to use exactly create method, not update
        task1.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 0,0));
        task1.setDuration(10L);

        task2.setId(null);
        task2.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 5,0));
        task2.setDuration(10L);

        String taskJson1 = gson.toJson(task1);
        String taskJson2 = gson.toJson(task2);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson1))
                .build();

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson2))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());
        assertEquals(406, response2.statusCode());
    }

    @Test
    @DisplayName("update task")
    void testUpdateTask() throws IOException, InterruptedException {
        task1.setId(1);
        String taskJson = gson.toJson(task1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("update task that intersect with existing task by execution time")
    void testUpdateTaskWithIntersect() throws IOException, InterruptedException {
        task1.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 0,0));
        task1.setDuration(10L);

        task2.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 5,0));
        task2.setDuration(10L);

        String taskJson1 = gson.toJson(task1);
        String taskJson2 = gson.toJson(task2);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson1))
                .build();

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson2))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());
        assertEquals(406, response2.statusCode());
    }

    @Test
    @DisplayName("delete task by id")
    void testDeleteTaskById() throws IOException, InterruptedException {
        int taskId = 1;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + taskId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertThrows(TaskNotFoundException.class, () -> taskManager.getSubtaskById(taskId));
    }

    @Test
    @DisplayName("delete all tasks")
    void testDeleteAllTasks() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertEquals(0, taskManager.getAllTasks().size());
    }

    @Test
    @DisplayName("get all subtasks")
    void testGetSubtasks() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(gson.toJson(taskManager.getAllSubtasks()), response.body());
    }

    @Test
    @DisplayName("get subtask by id")
    void testGetSubtaskById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/4"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(gson.toJson(subtask1), response.body());
    }

    @Test
    @DisplayName("get subtask by id if subtask with this id doesn't exist")
    void testGetSubtaskByIdNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/999"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertEquals("There is no subtask with such ID", response.body());
    }

    @Test
    @DisplayName("create subtask")
    void testCreateSubtask() throws IOException, InterruptedException {
        String taskJson = gson.toJson(subtask1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("create subtask that intersect with existing task by execution time")
    void testCreateSubtaskWithIntersect() throws IOException, InterruptedException {
        subtask1.setId(null); // set in null to use exactly create method, not update
        subtask1.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 0,0));
        subtask1.setDuration(10L);

        subtask2.setId(null);
        subtask2.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 5,0));
        subtask2.setDuration(10L);

        String taskJson1 = gson.toJson(subtask1);
        String taskJson2 = gson.toJson(subtask2);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson1))
                .build();

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson2))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());
        assertEquals(406, response2.statusCode());
    }

    @Test
    @DisplayName("update subtask")
    void testUpdateSubtask() throws IOException, InterruptedException {
        subtask1.setId(4);
        String taskJson = gson.toJson(subtask1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("update subtask that intersect with existing task by execution time")
    void testUpdateSubtaskWithIntersect() throws IOException, InterruptedException {
        subtask1.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 0,0));
        subtask1.setDuration(10L);

        subtask2.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 5,0));
        subtask2.setDuration(10L);

        String taskJson1 = gson.toJson(subtask1);
        String taskJson2 = gson.toJson(subtask2);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson1))
                .build();

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson2))
                .build();

        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());
        assertEquals(406, response2.statusCode());
    }

    @Test
    @DisplayName("delete subtask by id")
    void testDeleteSubtaskById() throws IOException, InterruptedException {
        int subtaskId = 4;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + subtaskId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertThrows(TaskNotFoundException.class, () -> taskManager.getSubtaskById(subtaskId));
    }

    @Test
    @DisplayName("delete all subtasks")
    void testDeleteAllSubtasks() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertEquals(0, taskManager.getAllSubtasks().size());
    }

    @Test
    @DisplayName("get all epics")
    void testGetEpics() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(gson.toJson(taskManager.getAllEpics()), response.body());
    }

    @Test
    @DisplayName("get epic by id")
    void testGetEpicById() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics/3"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(gson.toJson(epic1), response.body());
    }

    @Test
    @DisplayName("get epic by id if epic with this id doesn't exist")
    void testGetEpicByIdNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics/999"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertEquals("There is no epic with such ID", response.body());
    }

    @Test
    @DisplayName("create epic")
    void testCreateEpic() throws IOException, InterruptedException {
        String taskJson = gson.toJson(epic1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("update epic")
    void testUpdateEpic() throws IOException, InterruptedException {
        epic1.setId(3);
        String taskJson = gson.toJson(epic1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
    }

    @Test
    @DisplayName("delete epic by id")
    void testDeleteEpicById() throws IOException, InterruptedException {
        int epicId = 3;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics/" + epicId))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertThrows(TaskNotFoundException.class, () -> taskManager.getEpicById(epicId));
    }

    @Test
    @DisplayName("delete all epics")
    void testDeleteAllEpics() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertEquals(0, taskManager.getAllEpics().size());
    }

    @Test
    @DisplayName("get epic's subtasks")
    void testGetEpicSubtasks() throws IOException, InterruptedException {
        int epicId = 3;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics/" + epicId + "/subtasks"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(gson.toJson(taskManager.getEpicById(epicId).getSubTasks()), response.body());
    }

    @Test
    @DisplayName("get epic's subtasks if this epic doesn't exist")
    void testGetEpicSubtaskMissingEpic() throws IOException, InterruptedException {
        int epicId = 100001;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics/" + epicId + "/subtasks"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("get prioritized tasks")
    void testPriotitisatedTasks() throws IOException, InterruptedException {
        task1.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 0,0));
        task1.setDuration(10L);

        task2.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 45,0));
        task2.setDuration(30L);

        subtask2.setStartTime(LocalDateTime.of(2024, 8, 18, 12, 15,0));
        subtask2.setDuration(15L);

        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(subtask2);
        tasks.add(task2);

        String taskJson1 = gson.toJson(task1);
        String taskJson2 = gson.toJson(subtask2);
        String taskJson3 = gson.toJson(task2);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson1))
                .build();

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson2))
                .build();

        HttpRequest request3 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(taskJson3))
                .build();

        client.send(request1, HttpResponse.BodyHandlers.ofString());
        client.send(request2, HttpResponse.BodyHandlers.ofString());
        client.send(request3, HttpResponse.BodyHandlers.ofString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(gson.toJson(tasks), response.body());
    }

    @Test
    @DisplayName("get tasks from history")
    void testHistoryTasks() throws IOException, InterruptedException {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task1);
        tasks.add(subtask2);
        tasks.add(task2);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/1"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/5"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpRequest request3 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/2"))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        client.send(request1, HttpResponse.BodyHandlers.ofString());
        client.send(request2, HttpResponse.BodyHandlers.ofString());
        client.send(request3, HttpResponse.BodyHandlers.ofString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(gson.toJson(tasks), response.body());
    }

}
