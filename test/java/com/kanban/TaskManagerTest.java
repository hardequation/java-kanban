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
        epic1.setStatus(TaskStatus.DONE);
        int epicId = taskManager.createTask(epic1);

        subtask1.setEpicId(epicId);
        subtask1.setStatus(TaskStatus.NEW);

        subtask2.setEpicId(epicId);
        subtask2.setStatus(TaskStatus.NEW);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.NEW, epic1.getStatus());
    }

    @Test
    void testEpicStatusAllDone() {
        epic1.setStatus(TaskStatus.NEW);
        int epicId = taskManager.createTask(epic1);

        subtask1.setEpicId(epicId);
        subtask1.setStatus(TaskStatus.DONE);
        subtask2.setEpicId(epicId);
        subtask2.setStatus(TaskStatus.DONE);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.DONE, epic1.getStatus());
    }

    @Test
    void testEpicStatusNewAndDone() {
        epic1.setStatus(TaskStatus.NEW);
        int epicId = taskManager.createTask(epic1);

        subtask1.setEpicId(epicId);
        subtask1.setStatus(TaskStatus.NEW);
        subtask2.setEpicId(epicId);
        subtask2.setStatus(TaskStatus.DONE);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.IN_PROGRESS, epic1.getStatus());
    }

    @Test
    void testEpicStatusAllInProgress() {
        epic1.setStatus(TaskStatus.NEW);
        int epicId = taskManager.createTask(epic1);

        subtask1.setEpicId(epicId);
        subtask1.setStatus(TaskStatus.IN_PROGRESS);
        subtask2.setEpicId(epicId);
        subtask2.setStatus(TaskStatus.IN_PROGRESS);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(TaskStatus.IN_PROGRESS, epic1.getStatus());
    }

    @Test
    void testGettingAndCleaningAllTasks() {
        epic1.addSubtask(subtask1);
        epic1.addSubtask(subtask2);

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
        epic1.addSubtask(subtask1);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);

        assertEquals(task1, taskManager.getTaskById(task1.getId()));
        assertEquals(subtask1, taskManager.getSubtaskById(subtask1.getId()));
        assertEquals(epic1, taskManager.getEpicById(epic1.getId()));
    }

    @Test
    void testUpdateTasks() {
        int taskId = taskManager.createTask(task1);
        int epicId = taskManager.createTask(epic1);
        subtask1.setEpicId(epicId);
        int subtaskId = taskManager.createTask(subtask1);

        task1.setName("Updated name 1");
        subtask1.setDescription("Updated description 2");
        subtask1.setStatus(TaskStatus.IN_PROGRESS);
        epic1.setStatus(TaskStatus.IN_PROGRESS);

        taskManager.updateTask(task1);
        taskManager.updateTask(subtask1);
        taskManager.updateTask(epic1);

        assertEquals("Updated name 1", taskManager.getTaskById(taskId).getName());
        assertEquals("Updated description 2", taskManager.getSubtaskById(subtaskId).getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getEpicById(epicId).getStatus());
    }

    @Test
    void testIdConflict() {
        Integer epicId = taskManager.createTask(epic1);
        Integer taskId = taskManager.createTask(task1);
        subtask1.setEpicId(epicId);
        Integer subtaskId = taskManager.createTask(subtask1);

        assertNotEquals(taskId, subtaskId);
    }

    @Test
    void testTaskImmutabilityUponAddition() throws TaskNotFoundException {
        int taskId = taskManager.createTask(task1);

        Task retrievedTask = taskManager.getTaskById(taskId);
        assertEquals(task1.getName(), retrievedTask.getName());
        assertEquals(task1.getDescription(), retrievedTask.getDescription());
        assertEquals(task1.getStatus(), retrievedTask.getStatus());
        assertEquals(task1.getId(), retrievedTask.getId());
    }

    @Test
    void testHistoryManagerSavesTaskState() throws TaskNotFoundException {
        int taskId = taskManager.createTask(task1);
        taskManager.getTaskById(taskId);

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
        int taskId1 = taskManager.createTask(task1);
        int taskId2 = taskManager.createTask(task2);
        int taskId3 = taskManager.createTask(epic1);
        int taskId4 = taskManager.createTask(epic2);

        taskManager.getTaskById(taskId1);
        taskManager.getTaskById(taskId2);
        taskManager.getEpicById(taskId3);
        taskManager.getEpicById(taskId4);

        assertEquals(4, historyManager.getHistory().size());

        taskManager.getTaskById(taskId1);
        taskManager.getEpicById(taskId3);

        assertEquals(4, historyManager.getHistory().size());
    }

    @Test
    void testRemovingTasks() throws TaskNotFoundException {
        int taskId = taskManager.createTask(task1);
        int epicId = taskManager.createTask(epic1);

        subtask1.setEpicId(epicId);
        int subtaskId = taskManager.createTask(subtask1);

        taskManager.removeTaskById(taskId);
        assertThrows(TaskNotFoundException.class, () -> taskManager.getTaskById(taskId));
        taskManager.removeSubtaskById(subtaskId);
        assertThrows(TaskNotFoundException.class, () -> taskManager.getSubtaskById(subtaskId));
        taskManager.removeEpicById(epicId);
        assertThrows(TaskNotFoundException.class, () -> taskManager.getEpicById(epicId));
    }

    @Test
    void testRemoveFromHistoryFunction() throws TaskNotFoundException {
        int taskId = taskManager.createTask(task1);
        int epicId = taskManager.createTask(epic1);

        taskManager.getTaskById(taskId);
        taskManager.getEpicById(epicId);

        assertEquals(2, historyManager.getHistory().size());

        historyManager.remove(taskId);
        assertEquals(1, historyManager.getHistory().size());
    }

    @Test
    void testCleanFunction() throws TaskNotFoundException {
        int taskId = taskManager.createTask(task1);
        int epicId = taskManager.createTask(epic1);
        subtask1.setEpicId(epicId);
        int subtaskId = taskManager.createTask(subtask1);

        taskManager.getTaskById(taskId);
        taskManager.getSubtaskById(subtaskId);
        taskManager.getEpicById(epicId);

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
        int epicId = taskManager.createTask(epic1);

        LocalDateTime subtaskStartTime1 = LocalDateTime.of(2024, 7, 1, 12, 0, 0, 0);
        subtask1.setEpicId(epicId);
        subtask1.setStartTime(subtaskStartTime1);
        subtask1.setDuration(10L);

        LocalDateTime subtaskStartTime2 = LocalDateTime.of(2024, 7, 1, 12, 11, 0, 0);
        subtask2.setEpicId(epicId);
        subtask2.setStartTime(subtaskStartTime2);
        subtask2.setDuration(15L);

        taskManager.createTask(subtask1);
        taskManager.createTask(subtask2);

        assertEquals(subtaskStartTime1, epic1.getStartTime());
        assertEquals(subtask1.getDuration() + subtask2.getDuration(), epic1.getDuration());
        assertEquals(subtaskStartTime2.plusMinutes(subtask2.getDuration()), epic1.getEndTime());
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
