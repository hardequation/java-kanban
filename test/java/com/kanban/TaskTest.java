package com.kanban;

import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    Task task1;
    Task task2;
    Subtask subtask1;
    Subtask subtask2;
    Epic epic1;
    Epic epic2;

    @BeforeEach
    void setup() {
        task1 = new Task("Task 1", "Task description 1", TaskStatus.NEW, null, LocalDateTime.parse("2021-12-21T21:21:21"), 220L);
        task2 = new Task("Task 2", "Task description 2", TaskStatus.NEW, null, LocalDateTime.parse("2022-01-21T21:21:21"), 180L);

        subtask1 = new Subtask("Subtask 1", "Subtask description 1", TaskStatus.NEW, null, null, LocalDateTime.parse("2022-02-21T21:21:21"), 120L);
        subtask2 = new Subtask("Subtask 2", "Subtask description 2", TaskStatus.NEW, null, null, LocalDateTime.parse("2022-03-21T21:21:21"), 100L);

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
    void calculateEpicDurationAndStartAndEndTimes() {
        LocalDateTime expectedStartTime = LocalDateTime.parse("2024-01-20T15:21:21");
        LocalDateTime expectedEndTime = LocalDateTime.parse("2024-01-23T11:21:21");
        long expectedDuration = Duration.between(expectedStartTime, expectedEndTime).toMinutes();

        Subtask subTask1 = new Subtask("n1", "d1", TaskStatus.NEW, 1, 5, LocalDateTime.parse("2024-01-21T10:21:21"), 120L);
        Subtask subTask2 = new Subtask("n2", "d2", TaskStatus.NEW, 2, 5, expectedEndTime, 90L);
        Subtask subTask3 = new Subtask("n3", "d3", TaskStatus.NEW, 3, 5, expectedStartTime, 210L);

        epic1.addSubtask(subTask1);
        epic1.addSubtask(subTask2);
        epic1.addSubtask(subTask3);

        assertEquals(expectedStartTime, epic1.getStartTime());
        assertEquals(expectedEndTime, epic1.getEndTime());
        assertEquals(expectedDuration, epic1.getDuration());
    }
}
