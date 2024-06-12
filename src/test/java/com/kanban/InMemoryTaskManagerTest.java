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

    Task task1;
    Task task2;
    Subtask subtask1;
    Subtask subtask2;
    Epic epic1;
    Epic epic2;

    @BeforeEach
    public void setup() {
        historyManager = Managers.getDefaultHistory();
        taskManager = new InMemoryTaskManager(historyManager);

        task1 = new Task("Task 1", "Description 1", TaskStatus.NEW);
        task2 = new Task("Task 2", "Description 2", TaskStatus.NEW);

        subtask1 = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW);
        subtask2 = new Subtask("Subtask 2", "Description 2", TaskStatus.NEW);

        epic1 = new Epic("Epic 1", "Description 1", TaskStatus.NEW, new ArrayList<>());
        epic2 = new Epic("Epic 2", "Description 2", TaskStatus.NEW, new ArrayList<>());
    }

    @Test
    public void testTasksEquality() {
        task1.setId(1);
        task2.setId(1);
        assertEquals(task1, task2);
    }

    @Test
    public void testSubtaskEquality() {
        subtask1.setId(1);
        subtask2.setId(1);
        assertEquals(subtask1, subtask2);
    }

    @Test
    public void testEpicEquality() {
        epic1.setId(1);
        epic2.setId(1);
        assertEquals(epic1, epic2);
    }

    @Test
    public void testEpicCannotContainItselfAsSubtask() {
        int epicId = 1;
        try {
            epic1.setId(epicId);
            epic1.addSubtask(epicId);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Epic can't contain subtask"));
        }
    }

    @Test
    public void testSubtaskCannotBeItsOwnEpic() {
        int subtaskId = 1;
        try {
            new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, subtaskId, subtaskId);
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
        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);
        taskManager.createTask(subtask1);

        epic1.addSubtask(2);
        taskManager.updateTask(epic1);

        assertEquals(task1, taskManager.getTaskById(task1.getId()));
        assertEquals(subtask1, taskManager.getSubtaskById(subtask1.getId()));
        assertEquals(epic1, taskManager.getEpicById(epic1.getId()));
    }

    @Test
    public void testIdConflict() throws Exception {
        Integer epicId = taskManager.createTask(epic1);
        subtask1.setEpicId(epicId);

        Integer taskId = taskManager.createTask(task1);
        Integer subtaskId = taskManager.createTask(subtask1);

        assertNotEquals(taskId, subtaskId);
    }

    @Test
    public void testTaskImmutabilityUponAddition() throws TaskNotFoundException {
        task1.setId(1);
        taskManager.createTask(task1);

        Task retrievedTask = taskManager.getTaskById(1);
        assertEquals(task1.getName(), retrievedTask.getName());
        assertEquals(task1.getDescription(), retrievedTask.getDescription());
        assertEquals(task1.getStatus(), retrievedTask.getStatus());
        assertEquals(task1.getId(), retrievedTask.getId());
    }

    @Test
    public void testHistoryManagerSavesTaskState() throws TaskNotFoundException {
        task1.setId(1);
        taskManager.createTask(task1);
        taskManager.getTaskById(1);

        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());

        Task retrievedTask = history.getFirst();
        assertEquals(task1.getName(), retrievedTask.getName());
        assertEquals(task1.getDescription(), retrievedTask.getDescription());
        assertEquals(task1.getStatus(), retrievedTask.getStatus());
        assertEquals(task1.getId(), retrievedTask.getId());
    }
}
