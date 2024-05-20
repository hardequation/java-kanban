import java.util.List;

public class Epic extends Task {
    List<Subtask> subTasks;

    public Epic(String name, String description, TaskStatus status, int id, List<Subtask> subtasks) {
        super(name, description, status, id);
        this.subTasks = subtasks;
    }

    public List<Subtask> getSubTasks() {
        return subTasks;
    }

    public TaskStatus getStatus() {
        if (subTasks.isEmpty()) {
            return status;
        }
        boolean isNew = true;
        boolean isDone = true;
        for (Subtask subTask: subTasks) {
            if (!TaskStatus.NEW.equals(subTask.getStatus())) {
                isNew = false;
            }
            if (!TaskStatus.DONE.equals(subTask.getStatus())) {
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
