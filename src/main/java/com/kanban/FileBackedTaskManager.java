package com.kanban;

import com.kanban.exception.ManagerSaveException;
import com.kanban.exception.WrongFileFormatException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.kanban.TaskType.SUBTASK;

@Slf4j
public class FileBackedTaskManager extends InMemoryTaskManager {

    @NonNull
    private final Path tasksFile;

    public static final String HEADER = "id,type,name,status,description,epic";

    private static final Integer TASK_MANDATORY_PARAM_CNT = 5;

    public FileBackedTaskManager(HistoryManager historyManager, Path tasksFile) {
        super(historyManager);
        this.tasksFile = tasksFile;
        loadFromFile();
    }

    private void setTaskCounter(int counter) {
        this.taskCounter = counter;
    }

    public static Task fromString(String line) {
        List<String> items = Arrays.stream(line.strip().split(",")).toList();

        if (items.size() < TASK_MANDATORY_PARAM_CNT) {
            throw new WrongFileFormatException("Wrong count of params for task in line: " + line
                    + " , format should be: 'id, task type, task name, task status, task description, epicId (if it's subtask)'");
        }

        try {
            Integer id = Integer.parseInt(items.get(0).strip());
            TaskType type = TaskType.valueOf(items.get(1).strip());
            String name = items.get(2).strip();
            TaskStatus status = TaskStatus.valueOf(items.get(3).strip());
            String description = items.get(4).strip();
            LocalDateTime startTime = null;
            Long duration = null;

            if (items.size() > 6 && !items.get(6).isBlank()) {
                startTime = LocalDateTime.parse(items.get(6).strip());
            }

            if (items.size() > 7 && !items.get(7).isBlank()) {
                duration = Long.parseLong(items.get(7).strip());
            }

            switch (type) {
                case TASK -> {
                    return new Task(name, description, status, id, startTime, duration);
                }

                case EPIC -> {
                    return new Epic(name, description, status, id, new HashSet<>());
                }

                case SUBTASK -> {
                    int epicId = Integer.parseInt(items.get(5).strip());
                    return new Subtask(name, description, status, id, epicId, startTime, duration);
                }

                default -> throw new WrongFileFormatException("Couldn't define task type from file");
            }
        } catch (Exception e) {
            throw new WrongFileFormatException("Can't parse task '" + line + "', because:\n" + e.getMessage());
        }

    }

    public static String toString(Task task) {
        return task.getId().toString() + ","
                + task.getType() + ","
                + task.getName() + ","
                + task.getStatus().toString() + ","
                + task.getDescription() + ",";
    }

    public static String toString(Epic task) {
        return toString((Task) task);
    }

    public static String toString(Subtask task) {
        return toString((Task) task) + task.getEpicId();
    }

    @Override
    public Integer createTask(Task task) {
        super.createTask(task);
        save();
        return task.getId();
    }

    @Override
    public Integer createTask(Epic epic) {
        super.createTask(epic);
        save();
        return epic.getId();
    }

    @Override
    public Integer createTask(Subtask subtask) {
        super.createTask(subtask);
        save();
        return subtask.getId();
    }

    @Override
    public void removeTaskById(Integer id) {
        super.removeTaskById(id);
        save();
    }

    @Override
    public void removeSubtaskById(Integer id) {
        super.removeSubtaskById(id);
        save();
    }

    @Override
    public void removeEpicById(Integer id) {
        super.removeEpicById(id);
        save();
    }

    private void save() {
        try (BufferedWriter writer = Files.newBufferedWriter(tasksFile, StandardCharsets.UTF_8)) {
            List<Task> tasks = getAllTasks();
            tasks.addAll(getAllSubtasks());
            tasks.addAll(getAllEpics());

            List<String> taskContent = new ArrayList<>();
            taskContent.add(HEADER);
            for (Task task : tasks) {
                switch (task.getType()) {
                    case SUBTASK -> taskContent.add(toString((Subtask) task));
                    case EPIC -> taskContent.add(toString((Epic) task));
                    case TASK -> taskContent.add(toString(task));
                    default -> log.warn("Wrong type for task: {}", task);
                }
            }
            writer.write(String.join("\n", taskContent));
        } catch (IOException e) {
            throw new ManagerSaveException("Saving to the task history failed");
        }
    }

    private void loadFromFile() {
        if (!Files.isRegularFile(tasksFile)) {
            log.warn("WARN: Unable to load from file {}", tasksFile);
            return;
        }
            List<String> tasks;
            try {
                tasks = Files.readAllLines(tasksFile);
            } catch (IOException e) {
                log.warn("Unable to read tasks from file '{}'", tasksFile);
                return;
            }

            if (tasks.isEmpty() || !HEADER.equals(tasks.getFirst())) {
                throw new WrongFileFormatException("Header should be '" + HEADER + "'");
            } else {
                tasks.removeFirst(); // remove header
            }

            int latestTaskCounter = 0;
            Task taskTmp;
            for (String taskString : tasks) {
                try {
                    taskTmp = fromString(taskString);
                } catch (WrongFileFormatException e) {
                   log.warn(e.getMessage());
                    continue;
                }

                switch (taskTmp) {
                    case Epic epic -> createTask(epic);
                    case Subtask subtask -> createTask(subtask);
                    default -> createTask(taskTmp);
                }

                if (taskTmp.getId() > latestTaskCounter) {
                    latestTaskCounter = taskTmp.getId();
                }
            }

            for (Subtask subtask: getAllSubtasks()) {
                Integer epicId = subtask.getEpicId();
                if (epics.containsKey(epicId)) {
                    epics.get(epicId).addSubtask(subtask);
                }
            }
            setTaskCounter(latestTaskCounter);

    }

    @Override
    public void cleanTasks() {
        super.cleanTasks();
        cleanTaskType(TaskType.TASK);
    }

    @Override
    public void cleanSubtasks() {
        super.cleanSubtasks();
        cleanTaskType(SUBTASK);
    }

    @Override
    public void cleanEpics() {
        super.cleanEpics();
        cleanTaskType(TaskType.EPIC);
    }

    private void cleanTaskType(TaskType type) {
        List<String> tasks;

        try {
            tasks = Files.readAllLines(tasksFile);
        } catch (IOException e) {
            log.warn("Unable to read tasks from file '{}'", tasksFile);
            return;
        }

        if (tasks.isEmpty() || !HEADER.equals(tasks.getFirst())) {
            throw new WrongFileFormatException("Header should be '" + HEADER + "', but it's: " + tasks.getFirst());
        } else {
            tasks.removeFirst(); // remove header
        }

        String filteredTasks = tasks.stream()
                .filter(task -> !Objects.requireNonNull(fromString(task)).getType().equals(type))
                .collect(Collectors.joining("\n"));

        if (!filteredTasks.isBlank()) {
            filteredTasks = HEADER + "\n" + filteredTasks;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(tasksFile, StandardCharsets.UTF_8)) {
            writer.write(filteredTasks);
        } catch (IOException e) {
            log.warn("Unable to clean task with type {}", type.toString());
        }
    }

}
