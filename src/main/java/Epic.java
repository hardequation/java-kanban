import java.util.List;

public class Epic extends Task {
    private List<Integer> subTaskIds;

    public Epic(String name, String description, TaskStatus status, int id, List<Integer> subtaskIds) {
        super(name, description, status, id);
        this.subTaskIds = subtaskIds;
    }

    public Epic(String name, String description, TaskStatus status, List<Integer> subtaskIds) {
        super(name, description, status);
        this.subTaskIds = subtaskIds;
    }

    public Epic(String name, String description, List<Integer> subtaskIds) {
        super(name, description);
        this.subTaskIds = subtaskIds;
    }

    public List<Integer> getSubTaskIds() {
        return subTaskIds;
    }

}
