import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TaskManager {
    final private Map<Integer, Task> tasks = new HashMap<>();
    final private Map<Integer, Subtask> subTasks = new HashMap<>();
    final private Map<Integer, Epic> epics = new HashMap<>();
    private int counter = 0;

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public List<Task> getAllSubtasks() {
        return new ArrayList<>(subTasks.values());
    }

    public List<Task> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    public void cleanTasks() {
        tasks.clear();
    }

    public void cleanSubtasks() {
        Set<Integer> subtaskIds = subTasks.keySet();

        for (Epic epic: epics.values()) {
            epic.getSubTaskIds().removeAll(subtaskIds);
        }

        subTasks.clear();
    }

    public void cleanEpics() {
        Set<Integer> epicIds = epics.keySet();

        for (Subtask subtask: subTasks.values()) {
            if (epicIds.contains(subtask.getEpicId())) {
                subTasks.remove(subtask.getId());
            }
        }

        epics.clear();
    }

    public Task getTaskById(int id) throws TaskNotFoundException {
        if (tasks.containsKey(id)) {
            return tasks.get(id);
        }
        throw new TaskNotFoundException("There is no task with such ID");
    }

    public Subtask getSubtaskById(int id) throws TaskNotFoundException {
        if (subTasks.containsKey(id)) {
            return subTasks.get(id);
        }
        throw new TaskNotFoundException("There is no subtask with such ID");
    }

    public Epic getEpicById(int id) throws TaskNotFoundException {
        if (epics.containsKey(id)) {
            return epics.get(id);
        }
        throw new TaskNotFoundException("There is no epic with such ID");
    }

    public void createTask(Task task) {
        task.setId(generateId());
        tasks.put(task.getId(), task);
    }

    public void createTask(Subtask task) {
        task.setId(generateId());
        subTasks.put(task.getId(), task);
    }

    public void createTask(Epic task) {
        task.setId(generateId());
        epics.put(task.getId(), task);
    }

    public void updateTask(Task task) throws TaskNotFoundException {
        if (tasks.containsKey(task.getId()) && tasks.get(task.getId()) != null) {
            tasks.put(task.getId(), task);
        } else {
            throw new TaskNotFoundException("There is no task with such ID");
        }
    }

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

    public void updateTask(Epic epic) throws TaskNotFoundException {
        int id = epic.getId();
        if (epics.containsKey(id) && epics.get(epic.getId()) != null) {
            epics.put(id, epic);
        } else {
            throw new TaskNotFoundException("There is no epic with such ID");
        }
    }
    public void removeTaskById(int id) {
        tasks.remove(id);
    }

    public void removeSubtaskById(int id) {
        for (Epic epics: epics.values()) {
            epics.getSubTaskIds().remove(id);
        }

        subTasks.remove(id);
    }

    public void removeEpicById(int id) {
        for (int subtaskId: epics.get(id).getSubTaskIds()) {
            subTasks.remove(subtaskId);
        }

        epics.remove(id);
    }

    public List<Subtask> getSubtasks(int epicId) throws TaskNotFoundException {
        if (!epics.containsKey(epicId)) {
            throw new TaskNotFoundException("This task id is not Epic id, try again");
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
}
