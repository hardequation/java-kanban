package com.kanban;

import com.kanban.exception.TaskNotFoundException;
import com.kanban.exception.WrongFileFormatException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest extends InMemoryTaskManager {
    private FileBackedTaskManager taskManager;
    private HistoryManager historyManager;

    Task task1;
    Task task2;
    Subtask subtask1;
    Subtask subtask2;
    Epic epic1;
    Epic epic2;

    @TempDir
    public Path tempDir;

    private Path tasksFileName;

    @BeforeEach
    void setup() throws IOException {

        tasksFileName = tempDir.resolve("tasks.csv");

        tempDir = Files.createTempDirectory("testDir");
        historyManager = Managers.getDefaultHistory();
        taskManager = new FileBackedTaskManager(historyManager, tasksFileName);


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
    void testGettingAndCleaningAllTasks() {
        task1.setId(1);
        task2.setId(2);

        epic1.setId(3);
        epic1.addSubtask(subtask1);
        epic1.addSubtask(subtask2);
        epic2.setId(4);

        subtask1.setId(5);
        subtask1.setEpicId(3);
        subtask2.setId(6);
        subtask2.setEpicId(3);

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

//    @Test
//    void testEpicCannotContainItselfAsSubtask() {
//        int epicId = 1;
//        try {
//            epic1.setId(epicId);
//            epic1.addSubtask((Task) epic1);
//        } catch (Exception e) {
//            assertTrue(e.getMessage().contains("Epic can't contain subtask"));
//        }
//    }

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
    void testManagersInitialization() {
        TaskManager manager = Managers.getDefault();
        HistoryManager historyManager1 = Managers.getDefaultHistory();
        assertNotNull(manager);
        assertNotNull(historyManager1);
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
    void testFileCreation() {
        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);

        taskManager.createTask(task1);
        taskManager.createTask(epic1);
        taskManager.createTask(subtask1);
        epic1.addSubtask(subtask1);
        taskManager.updateTask(epic1);

        taskManager.getTaskById(1);
        taskManager.getSubtaskById(2);
        taskManager.getEpicById(3);

        assertTrue(Files.isRegularFile(tasksFileName));
    }

    @Test
    void taskToStringTransformation() {
        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);

        String taskLine = FileBackedTaskManager.toString(task1);
        String subtaskLine = FileBackedTaskManager.toString(subtask1);
        String epicLine = FileBackedTaskManager.toString(epic1);

        String expectedTask = "1,TASK,Task 1,NEW,Task description 1,";
        String expectedSubtask = "2,SUBTASK,Subtask 1,NEW,Subtask description 1,3";
        String expectedEpic = "3,EPIC,Epic 1,NEW,Epic description 1,";

        assertEquals(expectedTask, taskLine);
        assertEquals(expectedSubtask, subtaskLine);
        assertEquals(expectedEpic, epicLine);
    }

    @Test
    void rightTaskFromStringTransformation() {
        String expectedStartTime1 = "2021-12-21T09:21:21";
        String expectedStartTime2 = "2022-01-21T11:21:21";

        Long expectedDuration1 = 120L;
        Long expectedDuration2 = 90L;
        Long expectedDuration3 = 360L;

        String taskLine = " 1, TASK, Task name , NEW  , Task description,  , " + expectedStartTime1 + "," + expectedDuration1;
        String subtaskLine = " 2, SUBTASK, SubTask name , IN_PROGRESS  , SubTask description,  3 , " + expectedStartTime2 + "," + expectedDuration2;
        String epicLine = " 3, EPIC, Epic name , DONE  , Epic description,   , ";

        Task task = FileBackedTaskManager.fromString(taskLine);
        Subtask subtask = (Subtask) FileBackedTaskManager.fromString(subtaskLine);
        Epic epic = (Epic) FileBackedTaskManager.fromString(epicLine);

        assertEquals(1, task.getId());
        assertEquals(2, subtask.getId());
        assertEquals(3, epic.getId());

        assertEquals(TaskStatus.NEW, task.getStatus());
        assertEquals(TaskStatus.IN_PROGRESS, subtask.getStatus());
        assertEquals(TaskStatus.DONE, epic.getStatus());

        assertEquals(TaskType.TASK, task.getType());
        assertEquals(TaskType.SUBTASK, subtask.getType());
        assertEquals(TaskType.EPIC, epic.getType());

        assertEquals("Task description", task.getDescription());
        assertEquals("SubTask description", subtask.getDescription());
        assertEquals("Epic description", epic.getDescription());

        assertEquals(3, subtask.getEpicId());

        assertEquals(LocalDateTime.parse(expectedStartTime1), task.getStartTime());
        assertEquals(LocalDateTime.parse(expectedStartTime2), subtask.getStartTime());
        assertNull(epic.getStartTime());

        assertEquals(expectedDuration1, task.getDuration());
        assertEquals(expectedDuration2, subtask.getDuration());
        assertNull(epic.getDuration());

        assertEquals(3, subtask.getEpicId());
    }

    @Test
    void wrongTaskFromStringTransformation() {
        String taskLine1 = " 1, TASK, Task name , NEW  ,   ";
        String taskLine2 = " wrongId, TASK, Task name , NEW  , Task description,";
        String subtaskLine = " 2, SUBTASK, SubTask name , SubTask description, ";

        assertThrows(WrongFileFormatException.class, () -> FileBackedTaskManager.fromString(taskLine1));
        assertThrows(WrongFileFormatException.class, () -> FileBackedTaskManager.fromString(taskLine2));
        assertThrows(WrongFileFormatException.class, () -> FileBackedTaskManager.fromString(subtaskLine));
    }

    @Test
    void loadFromFileTest() throws IOException {
        Path file = tempDir.resolve("someFile.csv");

        String taskLine = " 1, TASK, Task name , NEW  , Task description,   ";
        String subtaskLine = " 2, SUBTASK, SubTask name , IN_PROGRESS  , SubTask description,  3 ";
        String epicLine = " 3, EPIC, Epic name , DONE  , Epic description,   ";

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(FileBackedTaskManager.HEADER + "\n" + taskLine + "\n" + subtaskLine + "\n" + epicLine);
        }

        taskManager = new FileBackedTaskManager(historyManager, file);

        assertEquals(1, taskManager.getAllTasks().size());
        assertEquals(1, taskManager.getAllSubtasks().size());
        assertEquals(1, taskManager.getAllEpics().size());

        assertTrue(taskManager.getEpicById(3).getSubTasks().stream()
                .map(Task::getId)
                .toList()
                .contains(2));

        Files.delete(file);
    }

    @Test
    void idempotenceEqualManager() throws IOException {
        Path file = tempDir.resolve("someFile.csv");
        String taskLine = " 1, TASK, Task name , NEW  , Task description,   ";
        String subtaskLine = " 2, SUBTASK, SubTask name , IN_PROGRESS  , SubTask description,  3 ";
        String epicLine = " 3, EPIC, Epic name , DONE  , Epic description,   ";

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(FileBackedTaskManager.HEADER + "\n" + taskLine + "\n" + subtaskLine + "\n" + epicLine);
        }

        TaskManager manager1 = new FileBackedTaskManager(historyManager, file);
        TaskManager manager2 = new FileBackedTaskManager(historyManager, file);

        assertEquals(manager1.getAllEpics(), manager2.getAllEpics());
        assertEquals(manager1.getAllTasks(), manager2.getAllTasks());
        assertEquals(manager1.getAllSubtasks(), manager2.getAllSubtasks());
    }

    @Test
    void idempotenceNotEqualManager() throws IOException {
        Path file1 = tempDir.resolve("someFile1.csv");
        Path file2 = tempDir.resolve("someFile2.csv");

        String taskLine1 = " 1, TASK, Task name , NEW  , Task description,   ";
        String subtaskLine1 = " 2, SUBTASK, SubTask name , IN_PROGRESS  , SubTask description,  3 ";
        String epicLine1 = " 3, EPIC, Epic name , DONE  , Epic description,   ";

        String taskLine2 = "4, TASK, Task name , NEW  , Task description,   ";
        String subtaskLine2 = "5, SUBTASK, SubTask name , IN_PROGRESS  , SubTask description,  3 ";
        String epicLine2 = "6, EPIC, Epic name , DONE  , Epic description,   ";

        try (BufferedWriter writer = Files.newBufferedWriter(file1, StandardCharsets.UTF_8)) {
            writer.write(FileBackedTaskManager.HEADER + "\n" + taskLine1 + "\n" + subtaskLine1 + "\n" + epicLine1);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file2, StandardCharsets.UTF_8)) {
            writer.write(FileBackedTaskManager.HEADER + "\n" + taskLine2 + "\n" + subtaskLine2 + "\n" + epicLine2);
        }

        TaskManager manager1 = new FileBackedTaskManager(historyManager, file1);
        TaskManager manager2 = new FileBackedTaskManager(historyManager, file2);

        assertNotEquals(manager1.getAllEpics(), manager2.getAllEpics());
        assertNotEquals(manager1.getAllTasks(), manager2.getAllTasks());
        assertNotEquals(manager1.getAllSubtasks(), manager2.getAllSubtasks());
    }

    @Test
    void stateBeforeAndAfterSave() {
        Path file1 = tempDir.resolve("someFile1.csv");

        TaskManager manager1 = new FileBackedTaskManager(historyManager, file1);

        task1.setId(1);
        subtask1.setId(2);
        subtask1.setEpicId(3);
        epic1.setId(3);

        manager1.createTask(task1);
        manager1.createTask(epic1);
        manager1.createTask(subtask1);
        epic1.addSubtask(subtask1);
        manager1.updateTask(epic1);

        TaskManager manager2 = new FileBackedTaskManager(historyManager, file1);

        assertEquals(manager1.getAllEpics(), manager2.getAllEpics());
        assertEquals(manager1.getAllTasks(), manager2.getAllTasks());
        assertEquals(manager1.getAllSubtasks(), manager2.getAllSubtasks());
    }
}