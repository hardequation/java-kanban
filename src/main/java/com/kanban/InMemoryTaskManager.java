package com.kanban;

import com.kanban.exception.TaskNotFoundException;
import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class InMemoryTaskManager implements TaskManager {

    protected final HistoryManager historyManager;
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Subtask> subTasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
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
        }
        tasks.clear();
    }

    @Override
    public void cleanSubtasks() {
        for (Epic epic : epics.values()) {
            epic.getSubTasks().clear();
            epic.setStatus(getEpicStatus(epic));
        }

        for (Subtask task: subTasks.values()) {
            historyManager.remove(task.getId());
        }
        subTasks.clear();
    }

    @Override
    public void cleanEpics() {
        cleanSubtasks();
        for (Epic task: epics.values()) {
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

        Epic epic = epics.get(task.getEpicId());
        if (epic != null && !epic.getSubTasks().contains(task.getId())) {
            epic.addSubtask(task);
            epic.setStatus(getEpicStatus(epic));
        }
        return task.getId();
    }

    @Override
    public Integer createTask(Epic epic) {
        epic.getSubTasks().forEach(subtask -> subTasks.putIfAbsent(subtask.getId(), subtask));
        if (epic.getId() == null) {
            epic.setId(generateId());
        }
        epics.put(epic.getId(), epic);
        return epic.getId();
    }

    @Override
    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            tasks.replace(task.getId(), task);
        } else {
            throw new TaskNotFoundException("There is no task with such ID");
        }
    }

    @Override
    public void updateTask(Subtask subtask) {
        Integer id = subtask.getId();
        if (subTasks.containsKey(id)) {
            subTasks.replace(id, subtask);
            Epic epic = epics.get(subtask.getEpicId());
            epic.setStatus(getEpicStatus(epic));
        } else {
            throw new TaskNotFoundException("There is no subtask with such ID");
        }
    }

    @Override
    public void updateTask(Epic epic) {
        Integer id = epic.getId();
        if (epics.containsKey(id)) {
            Epic oldEpic = epics.get(id);
            oldEpic.setStatus(epic.getStatus());
            oldEpic.setName(epic.getName());
            oldEpic.setDescription(epic.getDescription());
        } else {
            throw new TaskNotFoundException("There is no epic with such ID");
        }
    }

    @Override
    public void removeTaskById(Integer id) {
        tasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void removeSubtaskById(Integer id) {
        Integer epicId = subTasks.get(id).getEpicId();
        if (epicId != null) {
            Epic epic = epics.get(epicId);
            epic.getSubTasks().remove(id);
            epic.setStatus(getEpicStatus(epic));
        }

        subTasks.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void removeEpicById(Integer id) {
        for (Subtask subtask : epics.get(id).getSubTasks()) {
            subTasks.remove(subtask.getId());
            historyManager.remove(subtask.getId());
        }

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

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }
}
