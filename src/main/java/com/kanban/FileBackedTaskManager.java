package com.kanban;

import com.kanban.exception.ManagerSaveException;
import com.kanban.exception.WrongFileFormatException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FileBackedTaskManager extends InMemoryTaskManager {

    private Path tasksFile;

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

            switch (type) {
                case TASK -> {
                    return new Task(name, description, status, id);
                }

                case EPIC -> {
                    return new Epic(name, description, status, id, new HashSet<>());
                }

                case SUBTASK -> {
                    int epicId = Integer.parseInt(items.get(5).strip());
                    return new Subtask(name, description, status, id, epicId);
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
        return task.getId();
    }

    @Override
    public Integer createTask(Epic epic) {
        super.createTask(epic);
        return epic.getId();
    }

    @Override
    public Integer createTask(Subtask subtask) {
        super.createTask(subtask);
        return subtask.getId();
    }

    @Override
    public Task getTaskById(int id) {
        Task task = super.getTaskById(id);
        save();
        return task;
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = super.getSubtaskById(id);
        save();
        return subtask;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = super.getEpicById(id);
        save();
        return epic;
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
            List<Task> tasks = getHistory();
            List<String> taskContent = new ArrayList<>();
            taskContent.add(HEADER);
            for (Task task : tasks) {
                switch (task.getType()) {
                    case SUBTASK -> taskContent.add(toString((Subtask) task));
                    case EPIC -> taskContent.add(toString((Epic) task));
                    case TASK -> taskContent.add(toString(task));
                    default -> System.out.println("Wrong type");
                }
            }
            writer.write(String.join("\n", taskContent));
        } catch (IOException e) {
            throw new ManagerSaveException("Saving to the task history failed");
        }
    }

    private void loadFromFile() {
        if (Files.isRegularFile(tasksFile)) {
            List<String> tasks;
            try {
                tasks = Files.readAllLines(tasksFile);
            } catch (IOException e) {
                System.out.println("Unable to read tasks from file '" + tasksFile.toString() + "'");
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
                    System.out.println(e.getMessage());
                    continue;
                }

                if (taskTmp instanceof Epic) {
                    createTask((Epic) taskTmp);
                } else if (taskTmp instanceof Subtask) {
                    createTask((Subtask) taskTmp);
                } else {
                    createTask(taskTmp);
                }

                if (taskTmp.getId() > latestTaskCounter) {
                    latestTaskCounter = taskTmp.getId();
                }
            }

            for (Subtask subtask: getAllSubtasks()) {
                Integer epicId = subtask.getEpicId();
                if (epics.containsKey(epicId)) {
                    epics.get(epicId).addSubtask(subtask.getId());
                }
            }
            setTaskCounter(latestTaskCounter);
        }
    }

    @Override
    public void cleanTasks() {
        super.cleanTasks();
        cleanTaskType(TaskType.TASK);
    }

    @Override
    public void cleanSubtasks() {
        super.cleanSubtasks();
        cleanTaskType(TaskType.SUBTASK);
    }

    @Override
    public void cleanEpics() {
        super.cleanEpics();
        cleanTaskType(TaskType.EPIC);
    }

    private void cleanTaskType(TaskType type) {
        super.cleanTasks();
        try {
            List<String> tasks;

            try {
                tasks = Files.readAllLines(tasksFile);
            } catch (IOException e) {
                System.out.println("Unable to read tasks from file '" + tasksFile.toString() + "'");
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
            }

        } catch (IOException e) {
            System.out.println("Unable to clean task with type " + type.toString());
        }
    }

}
