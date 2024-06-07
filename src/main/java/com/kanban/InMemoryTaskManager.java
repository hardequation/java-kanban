package com.kanban;

import com.kanban.tasks.Epic;
import com.kanban.tasks.Subtask;
import com.kanban.tasks.Task;

import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    final private Map<Integer, Task> tasks = new HashMap<>();
    final private Map<Integer, Subtask> subTasks = new HashMap<>();
    final private Map<Integer, Epic> epics = new HashMap<>();

    private static final int HISTORY_SIZE = 10;
    final private Deque<Task> history = new ArrayDeque<>(HISTORY_SIZE);
    private int counter = 0;

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public List<Task> getAllSubtasks() {
        return new ArrayList<>(subTasks.values());
    }

    @Override
    public List<Task> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void cleanTasks() {
        tasks.clear();
    }

    @Override
    public void cleanSubtasks() {
        Set<Integer> subtaskIds = subTasks.keySet();

        for (Epic epic: epics.values()) {
            epic.getSubTaskIds().removeAll(subtaskIds);
        }

        subTasks.clear();
    }

    @Override
    public void cleanEpics() {
        subTasks.clear();
        epics.clear();
    }

    @Override
    public Task getTaskById(int id) throws TaskNotFoundException {
        if (tasks.containsKey(id)) {
            Task task = tasks.get(id);
            if (history.size() >= HISTORY_SIZE) {
                history.removeFirst();
            }
            history.add(task);
            return task;
        }
        throw new TaskNotFoundException("There is no task with such ID");
    }

    @Override
    public Subtask getSubtaskById(int id) throws TaskNotFoundException {
        if (subTasks.containsKey(id)) {
            Subtask subtask = subTasks.get(id);
            if (history.size() >= HISTORY_SIZE) {
                history.removeFirst();
            }
            history.add(subtask);
            return subtask;
        }
        throw new TaskNotFoundException("There is no subtask with such ID");
    }

    @Override
    public Epic getEpicById(int id) throws TaskNotFoundException {
        if (epics.containsKey(id)) {
            Epic epic = epics.get(id);
            if (history.size() >= HISTORY_SIZE) {
                history.removeFirst();
            }
            history.add(epic);
            return epics.get(id);
        }
        throw new TaskNotFoundException("There is no epic with such ID");
    }

    @Override
    public void createTask(Task task) {
        task.setId(generateId());
        tasks.put(task.getId(), task);
    }

    @Override
    public void createTask(Subtask task) {
        task.setId(generateId());
        subTasks.put(task.getId(), task);
    }

    @Override
    public void createTask(Epic task) {
        task.setId(generateId());
        epics.put(task.getId(), task);
    }

    @Override
    public void updateTask(Task task) throws TaskNotFoundException {
        if (tasks.containsKey(task.getId()) && tasks.get(task.getId()) != null) {
            tasks.put(task.getId(), task);
        } else {
            throw new TaskNotFoundException("There is no task with such ID");
        }
    }

    @Override
    public void updateTask(Subtask subtask) throws TaskNotFoundException {
        int id = subtask.getId();
        if (subTasks.containsKey(id) && subTasks.get(id) != null) {
            subTasks.put(id, subtask);

            Epic epic = epics.get(subtask.getEpicId());
            epic.setStatus(getEpicStatus(epic.getId()));
        } else {
            throw new TaskNotFoundException("There is no subtask with such ID");
        }
    }

    @Override
    public void updateTask(Epic epic) throws TaskNotFoundException {
        int id = epic.getId();
        if (epics.containsKey(id) && epics.get(id) != null) {
            Epic oldEpic = epics.get(id);
            oldEpic.setStatus(epic.getStatus());
            oldEpic.setName(epic.getName());
            oldEpic.setDescription(epic.getDescription());
        } else {
            throw new TaskNotFoundException("There is no epic with such ID");
        }
    }

    @Override
    public void removeTaskById(int id) {
        tasks.remove(id);
    }

    @Override
    public void removeSubtaskById(int id) {
        for (Epic epics: epics.values()) {
            epics.getSubTaskIds().remove(id);
        }

        subTasks.remove(id);
    }

    @Override
    public void removeEpicById(int id) {
        for (int subtaskId: epics.get(id).getSubTaskIds()) {
            subTasks.remove(subtaskId);
        }

        epics.remove(id);
    }

    @Override
    public List<Subtask> getSubtasks(int epicId) throws TaskNotFoundException {
        if (!epics.containsKey(epicId)) {
            throw new TaskNotFoundException("This task id is not module.Epic id, try again");
        }

        List<Subtask> resultSubtasks = new ArrayList<>();
        for (int id: epics.get(epicId).getSubTaskIds()) {
            resultSubtasks.add(subTasks.get(id));
        }
        return resultSubtasks;
    }

    public int generateId() {
        return ++counter;
    }

    private TaskStatus getEpicStatus(int epicId) throws TaskNotFoundException {
        Epic epic = epics.get(epicId);
        if (epic.getSubTaskIds().isEmpty()) {
            return TaskStatus.DONE;
        }
        boolean isNew = true;
        boolean isDone = true;
        for (Integer id: epic.getSubTaskIds()) {
            if (isNew && !TaskStatus.NEW.equals(getTaskById(id).getStatus())) {
                isNew = false;
            }
            if (isDone && !TaskStatus.DONE.equals(getTaskById(id).getStatus())) {
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
        return history.stream().toList();
    }
}
