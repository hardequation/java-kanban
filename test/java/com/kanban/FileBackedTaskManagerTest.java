package com.kanban;

import com.kanban.controllers.FileBackedTaskManager;
import com.kanban.controllers.Managers;
import com.kanban.controllers.TaskManager;
import com.kanban.exception.WrongFileFormatException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import com.kanban.utils.TaskStatus;
import com.kanban.utils.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {

    @TempDir
    public Path tempDir;

    private Path tasksFileName;

    @BeforeEach
    public void setup() {
        super.setup();
        tasksFileName = tempDir.resolve("tasks.csv");

        try {
            tempDir = Files.createTempDirectory("testDir");
        } catch (IOException e) {
            throw new RuntimeException("Couldn't prepare setup for test: temporary dir wasn't created");
        }

        historyManager = Managers.getDefaultHistory();
        taskManager = new FileBackedTaskManager(historyManager, tasksFileName);
    }

    @Test
    void testFileCreation() {
        int taskId = taskManager.createTask(task1);
        int epicId = taskManager.createTask(epic1);
        subtask1.setEpicId(epicId);
        int subtaskId = taskManager.createTask(subtask1);

        taskManager.updateTask(epic1);

        taskManager.getTaskById(taskId);
        taskManager.getSubtaskById(subtaskId);
        taskManager.getEpicById(epicId);

        assertTrue(Files.isRegularFile(tasksFileName));
    }

    @Test
    void taskToStringTransformation() {
        task1.setId(1);
        subtask1.setId(2);
        int epicId = 3;
        subtask1.setEpicId(epicId);
        epic1.setId(epicId);

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

        assertEquals(manager1.getAllEpics(), manager2.getAllEpics()); // eventually id is generated
        assertEquals(manager1.getAllTasks(), manager2.getAllTasks());
        assertEquals(manager1.getAllSubtasks(), manager2.getAllSubtasks());
    }

    @Test
    void stateBeforeAndAfterSave() {
        Path file1 = tempDir.resolve("someFile1.csv");

        TaskManager manager1 = new FileBackedTaskManager(historyManager, file1);

        manager1.createTask(task1);
        int epicId = manager1.createTask(epic1);
        subtask1.setEpicId(epicId);
        manager1.createTask(subtask1);

        TaskManager manager2 = new FileBackedTaskManager(historyManager, file1);

        assertEquals(manager1.getAllEpics(), manager2.getAllEpics());
        assertEquals(manager1.getAllTasks(), manager2.getAllTasks());
        assertEquals(manager1.getAllSubtasks(), manager2.getAllSubtasks());
    }
}