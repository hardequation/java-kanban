package com.kanban.controllers;

import com.kanban.exception.WrongTaskLogicException;
import com.kanban.utils.TaskStatus;
import com.kanban.exception.PriorityTaskException;
import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;
import com.kanban.utils.TaskType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class InMemoryTaskManager implements TaskManager {

    protected static final String PRIORITY_EXCEPTION_MESSAGE = "Start time and end time intersect with other tasks: ";

    protected final HistoryManager historyManager;

    protected final Map<Integer, Task> tasks = new HashMap<>();

    protected final Map<Integer, Subtask> subTasks = new HashMap<>();

    protected final Map<Integer, Epic> epics = new HashMap<>();

    protected TreeSet<Task> prioritisedTasks = new TreeSet<>(Comparator.comparing(Task::getStartTime));

    protected int taskCounter = 0;

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    public InMemoryTaskManager() {
        this.historyManager = Managers.getDefaultHistory();
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subTasks.values());
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void cleanTasks() {
        for (Task task : tasks.values()) {
            historyManager.remove(task.getId());
            prioritisedTasks.remove(task);
        }
        tasks.clear();
    }

    @Override
    public void cleanSubtasks() {
        for (Epic epic : epics.values()) {
            epic.getSubTasks().clear();
            epic.setStatus(getEpicStatus(epic));
            epic.setDuration(null);
            epic.setStartTime(null);
            epic.setEndTime(null);
        }

        for (Subtask task : subTasks.values()) {
            historyManager.remove(task.getId());
            prioritisedTasks.remove(task);
        }
        subTasks.clear();
    }

    @Override
    public void cleanEpics() {
        cleanSubtasks();
        for (Epic task : epics.values()) {
            historyManager.remove(task.getId());
        }
        epics.clear();
    }

    @Override
    public Task getTaskById(int id) {
        final Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
            return task;
        }
        throw new TaskNotFoundException("There is no task with such ID");
    }

    @Override
    public Subtask getSubtaskById(int id) {
        if (subTasks.containsKey(id)) {
            Subtask subtask = subTasks.get(id);
            historyManager.add(subtask);
            return subtask;
        }
        throw new TaskNotFoundException("There is no subtask with such ID");
    }

    @Override
    public Epic getEpicById(int id) {
        if (epics.containsKey(id)) {
            Epic epic = epics.get(id);
            historyManager.add(epic);
            return epics.get(id);
        }
        throw new TaskNotFoundException("There is no epic with such ID");
    }

    @Override
    public Integer createTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            throw new WrongTaskLogicException("WARN: This task already exists");
        }
        task.setId(generateId());
        addToPrioritizedTasks(task);
        tasks.put(task.getId(), task);
        return task.getId();
    }

    @Override
    public Integer createTask(Subtask task) {
        if (subTasks.containsKey(task.getId())) {
            throw new WrongTaskLogicException("WARN: This subtask already exists");
        }
        if (task.getEpicId() == null) {
            throw new TaskNotFoundException("ERROR: Epic id of this subtask doesn't exist");
        }

        task.setId(generateId());

        addToPrioritizedTasks(task);
        subTasks.put(task.getId(), task);

        Epic epic = epics.get(task.getEpicId());
        if (epic != null && !epic.getSubTasks().contains(task)) {
            epic.addSubtask(task);
            epic.setStatus(getEpicStatus(epic));
        }
        return task.getId();
    }

    @Override
    public Integer createTask(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            throw new WrongTaskLogicException("WARN: This epic already exists");
        }
        int epicId = generateId();
        epic.getSubTasks().forEach(subtask -> {
            if (!subTasks.containsKey(subtask.getId())) {
                subtask.setId(generateId());
                subtask.setEpicId(epicId);
                subTasks.put(subtask.getId(), subtask);
                addToPrioritizedTasks(subtask);
            }
        });
        epic.setId(epicId);
        epics.put(epicId, epic);
        return epicId;
    }

    @Override
    public void updateTask(Task task) {
        if (!tasks.containsKey(task.getId())) {
            throw new TaskNotFoundException("There is no task with such ID: " + task.getId());
        }
        if (!rightPriority(task)) {
            throw new PriorityTaskException(PRIORITY_EXCEPTION_MESSAGE + task);
        }

        Integer id = task.getId();
        removeFromPrioritizedTasks(tasks.get(id));
        addToPrioritizedTasks(task);

        tasks.replace(id, task);
    }

    @Override
    public void updateTask(Subtask subtask) {
        if (!subTasks.containsKey(subtask.getId())) {
            throw new TaskNotFoundException("There is no task with such ID: " + subtask.getId());
        }
        if (subtask.getEpicId() == null) {
            throw new TaskNotFoundException("ERROR: Epic id of this subtask doesn't exist");
        }
        if (!epics.containsKey(subtask.getEpicId())) {
            throw new TaskNotFoundException("ERROR: Epic of this subtask doesn't exist");
        }
        if (!rightPriority(subtask)) {
            throw new PriorityTaskException(PRIORITY_EXCEPTION_MESSAGE + subtask);
        }
        Integer id = subtask.getId();
        removeFromPrioritizedTasks(subTasks.get(id));
        addToPrioritizedTasks(subtask);
        subTasks.replace(id, subtask);

        Epic epic = epics.get(subtask.getEpicId());
        epic.setStatus(getEpicStatus(epic));
        epics.put(subtask.getEpicId(), epic);
    }

    @Override
    public void updateTask(Epic epic) {
        if (!epics.containsKey(epic.getId())) {
            throw new TaskNotFoundException("There is no task with such ID: " + epic.getId());
        }
        epic.getSubTasks().forEach(subtask -> {
            Integer subId = subtask.getId();
            if (!subTasks.containsKey(subId)) {
                subtask.setId(generateId());
            }
            removeFromPrioritizedTasks(subTasks.get(subId));
            addToPrioritizedTasks(subtask);

            subTasks.put(subId, subtask);
        });

        Integer id = epic.getId();
        epics.replace(id, epic);
    }

    @Override
    public void removeTaskById(Integer id) {
        removeFromPrioritizedTasks(tasks.get(id));
        tasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void removeSubtaskById(Integer id) {
        Subtask subtask = subTasks.get(id);
        Integer epicId = subtask.getEpicId();
        if (epicId != null) {
            Epic epic = epics.get(epicId);
            epic.getSubTasks().remove(subtask);
            epic.setStatus(getEpicStatus(epic));
        }

        removeFromPrioritizedTasks(subtask);
        subTasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void removeEpicById(Integer id) {
        for (Subtask subtask : epics.get(id).getSubTasks()) {
            removeFromPrioritizedTasks(subtask);
            subTasks.remove(subtask.getId());
            historyManager.remove(subtask.getId());
        }

        epics.remove(id);
        historyManager.remove(id);
    }

    @Override
    public List<Subtask> getEpicSubtasks(int epicId) {
        if (!epics.containsKey(epicId)) {
            throw new TaskNotFoundException("This task id is not module.Epic id, try again");
        }

        return new ArrayList<>(epics.get(epicId).getSubTasks());
    }

    public int generateId() {
        return ++taskCounter;
    }

    private TaskStatus getEpicStatus(Epic epic) {
        if (epic.getSubTasks().isEmpty()) {
            return TaskStatus.DONE;
        }
        boolean isNew = true;
        boolean isDone = true;
        for (Subtask tmpSubtask : epic.getSubTasks()) {
            Subtask subtask = subTasks.get(tmpSubtask.getId());
            if (isNew && !TaskStatus.NEW.equals(subtask.getStatus())) {
                isNew = false;
            }
            if (isDone && !TaskStatus.DONE.equals(subtask.getStatus())) {
                isDone = false;
            }
        }

        if (isNew) {
            return TaskStatus.NEW;
        } else if (isDone) {
            return TaskStatus.DONE;
        } else {
            return TaskStatus.IN_PROGRESS;
        }
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return prioritisedTasks.stream().toList();
    }

    private void addToPrioritizedTasks(Task task) {
        if (task == null) {
            throw new PriorityTaskException("ERROR: unable to add null to prioritised tasks");
        }

        if (task.getType() == TaskType.EPIC) {
            throw new PriorityTaskException("ERROR: can't add epic to prioritised tasks");
        }

        if (!rightPriority(task)) {
            throw new PriorityTaskException(PRIORITY_EXCEPTION_MESSAGE + task);
        }

        if (task.getStartTime() != null) {
            prioritisedTasks.add(task);
        }
    }

    private void removeFromPrioritizedTasks(Task task) {
        if (task != null && task.getStartTime() != null) {
            prioritisedTasks.remove(task);
        }
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    public boolean prioritiesAreRight(Task task1, Task task2) {
        LocalDateTime start1 = task1.getStartTime();
        LocalDateTime start2 = task2.getStartTime();
        LocalDateTime end1 = task1.getEndTime();
        LocalDateTime end2 = task2.getEndTime();

        return (start1.isBefore(start2) && end1.isBefore(start2)) ||
                (start1.isAfter(start2) && end2.isBefore(start1));
    }

    public boolean rightPriority(Task task) {
        return prioritisedTasks.stream()
                .allMatch(t1 -> prioritiesAreRight(t1, task));
    }
}
