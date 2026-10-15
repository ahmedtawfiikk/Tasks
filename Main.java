import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        TaskManager manager = new TaskManager();
        Task task1 = new Task("Buy groceries", Set.of("shopping", "errands"), 1);
        Task task2 = new Task("Finish project", Set.of("work", "urgent"), 2);
        Task task3 = new Task("Call mom", Set.of("family"), 3);

        manager.addTask(task1);
        manager.addTask(task2);
        manager.addTask(task3);

        System.out.println("Add new tag 'today': " + task1.addTag("today"));
        System.out.println("Add duplicate tag 'shopping': " + task1.addTag("shopping"));

        manager.listTasks();
        manager.markTaskAsDone(2);
        manager.listTasks();
    }
}

class Task {

    private String title;
    private String status;
    private int id;
    private Set<String> tags = new HashSet<>();

    Task(String title, Set<String> tags, int id) {
        this.title = title;
        this.status = "pending";
        this.tags.addAll(tags);
        this.id = id;
    }

    public Set<String> getTags() {
        return tags;
    }

    public boolean addTag(String tag) {
        return tags.add(tag);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    public void complete() {
        status = "done";
    }
}

class TaskManager {
    private Map<Integer, Task> tasks = new HashMap<>();

    public void addTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public Task findTaskById(int id) {
        return tasks.get(id);
    }

    public void listTasks() {
        for (Task task : tasks.values()) {
            System.out.println("ID: " + task.getId() + ", Title: " + task.getTitle() + ", Status: " + task.getStatus() + ", Tags: " + task.getTags());
        }
    }

    public void markTaskAsDone(int id) {
        Task task = findTaskById(id);
        if (task != null) {
            task.complete();
            System.out.println("Task with ID " + id + " marked as done.");
        } else {
            System.out.println("Task with ID " + id + " not found.");
        }
    }
}
