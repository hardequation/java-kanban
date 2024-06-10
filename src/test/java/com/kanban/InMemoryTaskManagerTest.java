package com.kanban;

import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {
    private InMemoryTaskManager taskManager;
    private HistoryManager historyManager;

    @BeforeEach
    public void setup() {
        historyManager = new InMemoryHistoryManager();
        taskManager = new InMemoryTaskManager(historyManager);
    }

    @Test
    public void testTasksEquality() {
        Task task1 = new Task("Task 1", "Description 1", TaskStatus.NEW, 1);
        Task task2 = new Task("Task 2", "Description 2", TaskStatus.NEW, 1);
        assertEquals(task1, task2);
    }

    @Test
    public void testSubtaskEquality() throws Exception {
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, 1, 2);
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", TaskStatus.NEW, 1, 3);
        assertEquals(subtask1, subtask2);
    }

    @Test
    public void testEpicEquality() throws Exception {
        Epic epic1 = new Epic("Epic 1", "Description 1", TaskStatus.NEW, 1, new ArrayList<>());
        Epic epic2 = new Epic("Epic 2", "Description 2", TaskStatus.NEW, 1, new ArrayList<>());
        assertEquals(epic1, epic2);
    }

    @Test
    public void testEpicCannotContainItselfAsSubtask() throws Exception {
        Integer epicId = 1;
        try {
            Epic epic = new Epic("Epic 1", "Description 1", TaskStatus.NEW, epicId, List.of(epicId));
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Epic can't contain subtask"));
        }
    }

    @Test
    public void testSubtaskCannotBeItsOwnEpic() {
        Integer subtaskId = 1;
        try {
            Subtask subtask = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, subtaskId, subtaskId);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Subtask id can't be equal id of its epic id"));
        }
    }

    @Test
    public void testManagersInitialization() {
        TaskManager manager = Managers.getDefault();
        HistoryManager historyManager = Managers.getDefaultHistory();
        assertNotNull(manager);
        assertNotNull(historyManager);
    }

    @Test
    public void testAddAndFindTasksById() throws Exception {
        Task task = new Task("Task 1", "Description 1", TaskStatus.NEW, 1);
        Subtask subtask = new Subtask("Subtask 1", "Description 1", TaskStatus.IN_PROGRESS, 2, 3);
        Epic epic = new Epic("Epic 1", "Description 1", TaskStatus.NEW, 3, new ArrayList<>());

        taskManager.createTask(task);
        taskManager.createTask(epic);
        taskManager.createTask(subtask);

        assertEquals(task, taskManager.getTaskById(task.getId()));
        assertEquals(subtask, taskManager.getSubtaskById(subtask.getId()));
        assertEquals(epic, taskManager.getEpicById(epic.getId()));
    }

    @Test
    public void testIdConflict() throws TaskNotFoundException {
        Task task = new Task("Task 1", "Description 1", TaskStatus.NEW);

        Epic epic = new Epic("Epic name", "Epic description", TaskStatus.IN_PROGRESS);
        Integer epicId = taskManager.createTask(epic);

        Subtask subtask = new Subtask("Subtask 1", "Description 1", TaskStatus.IN_PROGRESS, epicId);

        Integer taskId = taskManager.createTask(task);
        Integer subtaskId = taskManager.createTask(subtask);

        assertNotEquals(taskId, subtaskId);
    }

    @Test
    public void testTaskImmutabilityUponAddition() throws TaskNotFoundException {
        Task task = new Task("Task 1", "Description 1", TaskStatus.NEW, 1);
        taskManager.createTask(task);

        Task retrievedTask = taskManager.getTaskById(1);
        assertEquals(task.getName(), retrievedTask.getName());
        assertEquals(task.getDescription(), retrievedTask.getDescription());
        assertEquals(task.getStatus(), retrievedTask.getStatus());
        assertEquals(task.getId(), retrievedTask.getId());
    }

    @Test
    public void testHistoryManagerSavesTaskState() throws TaskNotFoundException {
        Task task = new Task("Task 1", "Description 1", TaskStatus.NEW, 1);
        taskManager.createTask(task);
        taskManager.getTaskById(1);

        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());

        Task retrievedTask = history.getFirst();
        assertEquals(task.getName(), retrievedTask.getName());
        assertEquals(task.getDescription(), retrievedTask.getDescription());
        assertEquals(task.getStatus(), retrievedTask.getStatus());
        assertEquals(task.getId(), retrievedTask.getId());
    }
}
