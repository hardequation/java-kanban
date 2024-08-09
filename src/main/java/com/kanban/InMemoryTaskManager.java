package com.kanban;

import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;

import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;

public class InMemoryTaskManager implements TaskManager {

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
        for (Task task: tasks.values()) {
            historyManager.remove(task.getId());
            prioritisedTasks.remove(task);
        }
        tasks.clear();
    }

    @Override
    public void cleanSubtasks() {
        for (Epic epic : epics.values()) {
            prioritisedTasks.remove(epic); // No subtasks -> no start time of epic
            epic.getSubTasks().clear();
            epic.setStatus(getEpicStatus(epic));
        }

        for (Subtask task: subTasks.values()) {
            historyManager.remove(task.getId());
            prioritisedTasks.remove(task);
        }
        subTasks.clear();
    }

    @Override
    public void cleanEpics() {
        cleanSubtasks();
        for (Epic task: epics.values()) {
            prioritisedTasks.remove(task);
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
        if (task.getId() == null) {
            task.setId(generateId());
        }
        tasks.put(task.getId(), task);
        addToPrioritizedTasks(task);
        return task.getId();
    }

    @Override
    public Integer createTask(Subtask task) {
        if (task.getEpicId() == null) {
            throw new TaskNotFoundException("ERROR: Epic id of this subtask doesn't exist");
        }

        if (task.getId() == null) {
            task.setId(generateId());
        }

        subTasks.put(task.getId(), task);
        addToPrioritizedTasks(task);

        Epic epic = epics.get(task.getEpicId());
        if (epic != null && !epic.getSubTasks().contains(task)) {
            removeFromPrioritizedTasks(epic);
            epic.addSubtask(task);
            epic.setStatus(getEpicStatus(epic));
            addToPrioritizedTasks(epic);
        }
        return task.getId();
    }

    @Override
    public Integer createTask(Epic epic) {
        epic.getSubTasks().forEach(subtask -> {
            addToPrioritizedTasks(subtask);
            subTasks.put(subtask.getId(), subtask);
        });
        if (epic.getId() == null) {
            epic.setId(generateId());
        }
        epics.put(epic.getId(), epic);
        addToPrioritizedTasks(epic);
        return epic.getId();
    }

    @Override
    public void updateTask(Task task) {
        Integer id = task.getId();
        if (tasks.containsKey(id)) {
            removeFromPrioritizedTasks(tasks.get(id));
            addToPrioritizedTasks(task);

            tasks.replace(id, task);
        } else {
            throw new TaskNotFoundException("There is no task with such ID");
        }
    }

    @Override
    public void updateTask(Subtask subtask) {
        Integer id = subtask.getId();
        if (subTasks.containsKey(id)) {
            removeFromPrioritizedTasks(subTasks.get(id));
            addToPrioritizedTasks(subtask);
            subTasks.replace(id, subtask);

            Epic epic = epics.get(subtask.getEpicId());
            epic.setStatus(getEpicStatus(epic));
            epics.put(subtask.getEpicId(), epic);
        } else {
            throw new TaskNotFoundException("There is no subtask with such ID");
        }
    }

    @Override
    public void updateTask(Epic epic) {
        epic.getSubTasks().forEach(subtask -> {
            Integer subId = subtask.getId();
            removeFromPrioritizedTasks(subTasks.get(subId));
            addToPrioritizedTasks(subtask);

            subTasks.put(subId, subtask);
        });

        Integer id = epic.getId();
        if (epics.containsKey(id)) {
            removeFromPrioritizedTasks(epics.get(id));
            addToPrioritizedTasks(epic);

            epics.replace(id, epic);
        } else {
            throw new TaskNotFoundException("There is no epic with such ID");
        }
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
            removeFromPrioritizedTasks(epic);
            epic.getSubTasks().remove(subtask);
            epic.setStatus(getEpicStatus(epic));
            addToPrioritizedTasks(epic);
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

        removeFromPrioritizedTasks(epics.get(id));
        epics.remove(id);
        historyManager.remove(id);
    }

    @Override
    public List<Subtask> getSubtasks(int epicId) {
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

    protected List<Task> getPrioritizedTasks() {
        return prioritisedTasks.stream().toList();
    }

    private void addToPrioritizedTasks(Task task) {
        if (task != null && task.getStartTime() != null) {
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
}
