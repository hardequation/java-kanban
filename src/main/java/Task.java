import java.util.Objects;

public class Task {
    final protected String name;
    final protected String description;
    final protected TaskStatus status;

    final protected int id;

    public Task(String name, String description, TaskStatus status, int id) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.status = status;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
