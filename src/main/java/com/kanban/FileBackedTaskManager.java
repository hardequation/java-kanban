package com.kanban;

import com.kanban.exception.ManagerSaveException;
import com.kanban.exception.WrongFileFormatException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FileBackedTaskManager extends InMemoryTaskManager {

    private Path tasksFile;

    private static final Integer TASK_MANDATORY_PARAM_CNT = 5;

    public FileBackedTaskManager(HistoryManager historyManager, Path tasksFile) {
        super(historyManager);
        this.tasksFile = tasksFile;
        try {
            if (Files.isRegularFile(tasksFile)) {
                List<String> tasks = Files.readAllLines(tasksFile);

                int latestTaskCounter = 0;
                for (String taskString: tasks) {
                    Task task = fromString(taskString);

                    historyManager.add(task);
                    if (task.getId() > latestTaskCounter) {
                        latestTaskCounter = task.getId();
                    }
                }
                setTaskCounter(latestTaskCounter);
            }

        } catch (IOException e) {
            throw new ManagerSaveException("");
        }

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
                    return new Task(name, description, status, id, TaskType.TASK);
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
            throw new WrongFileFormatException("Can't parse task '" + line + "', because:\n" +  e.getMessage());
        }

    }

    public static String toString(Task task) {
        return task.getId().toString() + ","
                + "TASK" + ","
                + task.getName() + ","
                + task.getStatus().toString() + ","
                + task.getDescription() + ",";
    }

    public static String toString(Epic task) {
        return task.getId().toString() + ","
                + "EPIC" + ","
                + task.getName() + ","
                + task.getStatus().toString() + ","
                + task.getDescription() + ",";
    }

    public static String toString(Subtask task) {
        return task.getId().toString() + ","
                + "SUBTASK" + ","
                + task.getName() + ","
                + task.getStatus().toString() + ","
                + task.getDescription() + ","
                + task.getEpicId();
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
            for (Task task: tasks) {
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
//        try (FileWriter writer = new FileWriter(tasksFile)) {
//            List<Task> tasks = getHistory();
//            List<String> taskContent = new ArrayList<>();
//            for (Task task: tasks) {
//                switch (task.getType()) {
//                    case SUBTASK -> taskContent.add(toString((Subtask) task));
//                    case EPIC -> taskContent.add(toString((Epic) task));
//                    case TASK -> taskContent.add(toString(task));
//                    default -> System.out.println("Wrong type");
//                }
//            }
//            writer.write(String.join("\n", taskContent));
//        } catch (IOException e) {
//            throw new ManagerSaveException("Saving to the task history failed");
//        }

    }

    private void loadFromFile() {
        try {
            List<String> tasks = Files.readAllLines(tasksFile);

            int latestTaskCounter = 0;
            for (String taskString: tasks) {
                Task task = fromString(taskString);
                historyManager.add(task);
                if (task.getId() > latestTaskCounter) {
                    latestTaskCounter = task.getId();
                }
            }
            setTaskCounter(latestTaskCounter);

        } catch (IOException e) {
            throw new ManagerSaveException("");
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
            List<String> tasks = Files.readAllLines(tasksFile);

            String filteredTasks = tasks.stream()
                    .filter(task -> !Objects.requireNonNull(fromString(task)).getType().equals(type))
                    .collect(Collectors.joining("\n"));

            try (BufferedWriter writer = Files.newBufferedWriter(tasksFile, StandardCharsets.UTF_8)) {
                writer.write(filteredTasks);
            }

        } catch (IOException e) {
            System.out.println("Unable to clean task with type " + type.toString());
        }
    }

}
