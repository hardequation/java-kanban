import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskManager {
    final private Map<Integer, Task> tasks = new HashMap<>();
    final private Map<Integer, Subtask> subTasks = new HashMap<>();
    final private Map<Integer, Epic> epics = new HashMap<>();
    private int counter = 0;

    public List<Task> getTasksList() {
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(tasks.values().stream().toList());
        allTasks.addAll(subTasks.values().stream().toList());
        allTasks.addAll(epics.values().stream().toList());
        return allTasks;
    }

    public void cleanTasks() {
        tasks.clear();
        subTasks.clear();
        epics.clear();
    }

    public Task getTaskById(int id) throws Exception {
        if (tasks.containsKey(id)) {
            return tasks.get(id);
        }

        if (subTasks.containsKey(id)) {
            return subTasks.get(id);
        }

        if (epics.containsKey(id)) {
            return epics.get(id);
        }
        throw new Exception("Such id doesn't exist");
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

    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId()) && tasks.get(task.getId()) != null) {
            tasks.put(task.getId(), task);
        } else {
            System.out.println("There is no such task!");
        }
    }

    public void updateTask(Subtask subtask) throws Exception {
        int id = subtask.getId();
        if (subTasks.containsKey(id) && subTasks.get(id) != null) {
            subTasks.put(id, subtask);

            Epic epic = epics.get(subtask.getEpicId());
            epic.setStatus(getEpicStatus(epic.getId()));
        } else {
            System.out.println("There is no such task!");
        }
    }

    private TaskStatus getEpicStatus(int epicId) throws Exception {
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
    public void removeTaskById(int id) {
        if (epics.containsKey(id)) {
            for (Subtask subtask: subTasks.values()) {
                if (subtask.getEpicId() == id) {
                    subTasks.remove(subtask.getId());
                }
            }
            epics.remove(id);
            return;
        }
        tasks.remove(id);
        subTasks.remove(id);
    }

    public List<Subtask> getSubtasks(int epicId) throws Exception {
        if (!epics.containsKey(epicId)) {
            throw new Exception("This task id is not Epic id, try again");
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
}
