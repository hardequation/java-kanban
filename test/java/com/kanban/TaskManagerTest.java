package com.kanban;

import com.kanban.controllers.HistoryManager;
import com.kanban.controllers.Managers;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.PriorityTaskException;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import com.kanban.utils.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class TaskManagerTest<T extends TaskManager> {
    protected T taskManager;
    protected HistoryManager historyManager;

    protected Task task1;
    protected Task task2;
    protected Subtask subtask1;
    protected Subtask subtask2;
    protected Epic epic1;
    protected Epic epic2;

    @BeforeEach
    public void setup() {
        task1 = new Task("Task 1", "Task description 1", TaskStatus.NEW);
        task2 = new Task("Task 2", "Task description 2", TaskStatus.NEW);

        subtask1 = new Subtask("Subtask 1", "Subtask description 1", TaskStatus.NEW);
        subtask2 = new Subtask("Subtask 2", "Subtask description 2", TaskStatus.NEW);

        epic1 = new Epic("Epic 1", "Epic description 1", TaskStatus.NEW, new HashSet<>());
        epic2 = new Epic("Epic 2", "Epic description 2", TaskStatus.NEW, new HashSet<>());
    }

    @Test
    void testTasksEquality() {
        task1.setId(1);
        Task task = new Task(task1.getName(), task1.getDescription(), task1.getStatus(), task1.getId());
        assertEquals(task1, task);
    }

    @Test
    void testSubtaskEquality() {
        subtask1.setId(1);
        subtask1.setEpicId(3);
        Subtask subtask = new Subtask(
                subtask1.getName(),
                subtask1.getDescription(),
                subtask1.getStatus(),
                subtask1.getId(),
                subtask1.getEpicId());
        assertEquals(subtask1, subtask);
    }

    @Test
    void testEpicEquality() {
        epic1.setId(1);
        Epic epic = new Epic(
                epic1.getName(),
                epic1.getDescription(),
                epic1.getStatus(),
                epic1.getId(),
                epic1.getSubTasks());
        assertEquals(epic1, epic);
    }

    @Test
    void testSubtaskCannotBeItsOwnEpic() {
        int subtaskId = 1;
        try {
            new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, subtaskId, subtaskId);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Subtask id can't be equal id of its epic id"));
        }
    }

    @Test
    void testEpicStatusAllNew() {
        epic1.setId(1);
        epic1.setStatus(TaskStatus.DONE);
        taskManager.createTask(epic1);

        subtask1.setEpicId(1);
        subtask1.setStatus(TaskStatus.NEW);
        subtask2.setEpicId(1);
        subtask2.setStatus(TaskStatus.NEW);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.NEW, epic1.getStatus());
    }

    @Test
    void testEpicStatusAllDone() {
        epic1.setId(1);
        epic1.setStatus(TaskStatus.NEW);
        taskManager.createTask(epic1);

        subtask1.setEpicId(1);
        subtask1.setStatus(TaskStatus.DONE);
        subtask2.setEpicId(1);
        subtask2.setStatus(TaskStatus.DONE);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.DONE, epic1.getStatus());
    }

    @Test
    void testEpicStatusNewAndDone() {
        epic1.setId(1);
        epic1.setStatus(TaskStatus.NEW);
        taskManager.createTask(epic1);

        subtask1.setEpicId(1);
        subtask1.setStatus(TaskStatus.NEW);
        subtask2.setEpicId(1);
        subtask2.setStatus(TaskStatus.DONE);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.IN_PROGRESS, epic1.getStatus());
    }

    @Test
    void testEpicStatusAllInProgress() {
        epic1.setId(1);
        epic1.setStatus(TaskStatus.NEW);
        taskManager.createTask(epic1);

        subtask1.setEpicId(1);
        subtask1.setStatus(TaskStatus.IN_PROGRESS);
        subtask2.setEpicId(1);
        subtask2.setStatus(TaskStatus.IN_PROGRESS);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.IN_PROGRESS, epic1.getStatus());
    }

    @Test
    void testGettingAndCleaningAllTasks() {
        task1.setId(1);
        task2.setId(2);

        subtask1.setId(5);
        subtask1.setEpicId(3);
        subtask2.setId(6);
        subtask2.setEpicId(3);

        epic1.setId(3);
        epic1.addSubtask(subtask1);
        epic1.addSubtask(subtask2);
        epic2.setId(4);

        taskManager.createTask(task1);
        taskManager.createTask(task2);

        taskManager.createTask(epic1);
        taskManager.createTask(epic2);

        taskManager.updateTask(epic1);

        assertEquals(2, taskManager.getAllTasks().size());
        assertEquals(2, taskManager.getAllSubtasks().size());
        assertEquals(2, taskManager.getAllEpics().size());

        taskManager.cleanEpics();
        taskManager.cleanSubtasks();
        taskManager.cleanTasks();

        assertEquals(0, taskManager.getAllTasks().size());
        assertEquals(0, taskManager.getAllSubtasks().size());
        assertEquals(0, taskManager.getAllEpics().size());
    }

    @Test
    void testManagersInitialization() {
        TaskManager manager = Managers.getDefault();
        HistoryManager historyManager1 = Managers.getDefaultHistory();
        assertNotNull(manager);
        assertNotNull(historyManager1);
        assertEquals(0, manager.getAllTasks().size());
        assertEquals(0, manager.getAllSubtasks().size());
        assertEquals(0, manager.getAllEpics().size());
        assertEquals(0, historyManager1.getHistory().size());
    }

    @Test
    void testAddAndFindTasksById() {
        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);
        epic1.addSubtask(subtask1);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);

        taskManager.updateTask(epic1);

        assertEquals(task1, taskManager.getTaskById(task1.getId()));
        assertEquals(subtask1, taskManager.getSubtaskById(subtask1.getId()));
        assertEquals(epic1, taskManager.getEpicById(epic1.getId()));
    }

    @Test
    void testUpdateTasks() {
        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);
        taskManager.createTask(subtask1);

        task1.setName("Updated name 1");
        subtask1.setDescription("Updated description 2");
        subtask1.setStatus(TaskStatus.IN_PROGRESS);
        epic1.setStatus(TaskStatus.IN_PROGRESS);

        taskManager.updateTask(task1);
        taskManager.updateTask(subtask1);
        taskManager.updateTask(epic1);

        assertEquals("Updated name 1", taskManager.getTaskById(1).getName());
        assertEquals("Updated description 2", taskManager.getSubtaskById(2).getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getEpicById(3).getStatus());
    }

    @Test
    void testIdConflict() {
        Integer epicId = taskManager.createTask(epic1);
        subtask1.setEpicId(epicId);

        Integer taskId = taskManager.createTask(task1);
        Integer subtaskId = taskManager.createTask(subtask1);

        assertNotEquals(taskId, subtaskId);
    }

    @Test
    void testTaskImmutabilityUponAddition() throws TaskNotFoundException {
        task1.setId(1);
        taskManager.createTask(task1);

        Task retrievedTask = taskManager.getTaskById(1);
        assertEquals(task1.getName(), retrievedTask.getName());
        assertEquals(task1.getDescription(), retrievedTask.getDescription());
        assertEquals(task1.getStatus(), retrievedTask.getStatus());
        assertEquals(task1.getId(), retrievedTask.getId());
    }

    @Test
    void testHistoryManagerSavesTaskState() throws TaskNotFoundException {
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

    @Test
    void testRepeatedTasksInHistory() throws TaskNotFoundException {
        task1.setId(1);
        task2.setId(2);
        epic1.setId(3);
        epic2.setId(4);
        taskManager.createTask(task1);
        taskManager.createTask(task2);
        taskManager.createTask(epic1);
        taskManager.createTask(epic2);

        taskManager.getTaskById(1);
        taskManager.getTaskById(2);
        taskManager.getEpicById(3);
        taskManager.getEpicById(4);

        assertEquals(4, historyManager.getHistory().size());

        taskManager.getTaskById(1);
        taskManager.getEpicById(3);

        assertEquals(4, historyManager.getHistory().size());
    }

    @Test
    void testRemovingTasks() throws TaskNotFoundException {
        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);
        epic1.addSubtask(subtask1);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);

        taskManager.updateTask(epic1);

        taskManager.removeTaskById(1);
        assertThrows(TaskNotFoundException.class, () -> taskManager.getTaskById(1));
        taskManager.removeSubtaskById(2);
        assertThrows(TaskNotFoundException.class, () -> taskManager.getSubtaskById(2));
        taskManager.removeEpicById(3);
        assertThrows(TaskNotFoundException.class, () -> taskManager.getEpicById(3));
    }

    @Test
    void testRemoveFunction() throws TaskNotFoundException {
        task1.setId(1);
        epic1.setId(2);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);

        taskManager.getTaskById(1);
        taskManager.getEpicById(2);

        assertEquals(2, historyManager.getHistory().size());

        historyManager.remove(1);
        assertEquals(1, historyManager.getHistory().size());
    }

    @Test
    void testCleanFunction() throws TaskNotFoundException {
        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);
        epic1.addSubtask(subtask1);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);

        taskManager.updateTask(epic1);

        taskManager.getTaskById(1);
        taskManager.getSubtaskById(2);
        taskManager.getEpicById(3);

        assertEquals(3, historyManager.getHistory().size());

        taskManager.cleanTasks();
        assertEquals(2, historyManager.getHistory().size());
        taskManager.cleanSubtasks();
        assertEquals(1, historyManager.getHistory().size());
        taskManager.cleanEpics();
        assertEquals(0, historyManager.getHistory().size());
    }

    @Test
    void testEpicPriority() {
        epic1.setId(1);
        taskManager.createTask(epic1);

        subtask1.setEpicId(1);
        subtask1.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 0, 0, 0));
        subtask1.setDuration(10L);

        subtask2.setEpicId(1);
        subtask2.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 11, 0, 0));
        subtask2.setDuration(15L);
        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(LocalDateTime.of(2024, 7, 1, 12, 0, 0, 0), epic1.getStartTime());
        assertEquals(26L, epic1.getDuration());
        assertEquals(LocalDateTime.of(2024, 7, 1, 12, 26, 0, 0), epic1.getEndTime());
    }

    @Test
    void testPriorities() {
        task1.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 0, 0, 0));
        task1.setDuration(10L);

        task2.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 11, 0, 0));
        task2.setDuration(10L);

        taskManager.createTask(task1);
        taskManager.createTask(task2);

        assertEquals(task1, taskManager.getPrioritizedTasks().get(0));
        assertEquals(task2, taskManager.getPrioritizedTasks().get(1));
    }

    @Test
    void testOneTaskInsideOther() {
        task1.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 0, 0, 0));
        task1.setDuration(20L);

        task2.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 1, 0, 0));
        task2.setDuration(10L);

        taskManager.createTask(task1);
        assertThrows(PriorityTaskException.class, () -> taskManager.createTask(task2));

        taskManager.cleanTasks();
        taskManager.createTask(task2);
        assertThrows(PriorityTaskException.class, () -> taskManager.createTask(task1));
    }

    @Test
    void testIntersectedTasks() {
        task1.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 0, 0, 0));
        task1.setDuration(20L);

        task2.setStartTime(LocalDateTime.of(2024, 7, 1, 12, 10, 0, 0));
        task2.setDuration(40L);

        taskManager.createTask(task1);
        assertThrows(PriorityTaskException.class, () -> taskManager.createTask(task2));
    }
}
