import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskManager {

    private Map<Integer, Task> tasks = new HashMap<>();
    static int counter = 0;

    public List<Task> getTasksList() {
        return tasks.values().stream().toList();
    }

    public void cleanTasks() {
        tasks.clear();
    }

    public Task getTaskById(int id) {
        return tasks.get(id);
    }

    public void createTask(Task task) {
        Task newTask = new Task(
                task.getName(),
                task.getDescription(),
                task.getStatus(),
                task.getId());
        tasks.put(task.getId(), newTask);
    }

    public void createTask(Epic epic) {
        Epic newEpic = new Epic(
                epic.getName(),
                epic.getDescription(),
                epic.getStatus(),
                epic.getId(),
                epic.getSubTasks());
        tasks.put(epic.getId(), newEpic);
    }

    public void createTask(Subtask subTask) {
        Subtask newSubtask = new Subtask(
                subTask.getName(),
                subTask.getDescription(),
                subTask.getStatus(),
                subTask.getId(),
                subTask.getEpicId());
        tasks.put(subTask.getId(), newSubtask);
    }

    public void updateTask(Subtask subtask) {
        tasks.put(subtask.getId(), subtask);
        Epic epic = (Epic) tasks.get(subtask.getEpicId());
        
        Epic updated = new Epic(
                epic.getName(),
                epic.getDescription(),
                epic.getStatus(),
                epic.getId(),
                epic.getSubTasks());
        tasks.put(updated.getId(), updated);
    }
    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
    }
    public void removeTaskById(int id) {
        tasks.remove(id);
    }

    public List<Subtask> getSubtasks(int epicId) {
        if (!(tasks.get(epicId) instanceof Epic)) {
            System.out.println("This task id is not Epic id, try again");
            return new ArrayList<>();
        }
        return ((Epic) tasks.get(epicId)).getSubTasks();
    }

    public static int generateId() {
        counter++;
        return counter;
    }
}
