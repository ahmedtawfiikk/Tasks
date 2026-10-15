import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        TaskManager manager = new TaskManager();
        manager.addTask(new Task("Buy groceries", Set.of("shopping", "errands"), 1));
        manager.addTask(new Task("Finish project", Set.of("work", "urgent"), 2));
        manager.addTask(new Task("Call mom", Set.of("family"), 3));

        Task task1 = manager.findTaskById(1);
        System.out.println("Add new tag 'today': " + task1.addTag("today"));
        System.out.println("Add duplicate tag 'shopping': " + task1.addTag("shopping"));

        // Factory tasks must be added to the manager too
        TaskFactory factory = new TaskFactory();
        manager.addTask(factory.createBugTask("Fix login bug", Set.of("bug", "urgent"), 4, Severity.HIGH));
        manager.addTask(factory.createFeatureTask("Add dark mode", Set.of("feature"), 5, 3.5f));

        manager.markTaskAsDone(2);

        System.out.println("\n--- Sorted by ID ---");
        manager.listTasks(new SortById());

        System.out.println("\n--- Sorted by title ---");
        manager.listTasks(new SortByTitle());

        System.out.println("\n--- Sorted by status (pending first) ---");
        manager.listTasks(new SortByStatus());
    }
}


enum Status {
    PENDING, DONE        // declaration order = sort order
}

enum Severity {
    LOW, MEDIUM, HIGH
}


class Task {
    private String title;
    private Status status = Status.PENDING;   // every new task starts as PENDING
    private int id;
    private Set<String> tags = new HashSet<>();

    Task(String title, Set<String> tags, int id) {
        this.title = title;
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

    public Status getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    public void complete() {
        status = Status.DONE;
    }
}


class BugTask extends Task {
    private Severity severity;

    public BugTask(String title, Set<String> tags, int id, Severity severity) {
        super(title, tags, id);
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }
}


class FeatureTask extends Task {
    private float estimatedTime;

    public FeatureTask(String title, Set<String> tags, int id, float estimatedTime) {
        super(title, tags, id);
        this.estimatedTime = estimatedTime;
    }

    public float getEstimatedTime() {
        return estimatedTime;
    }
}


class TaskFactory {

    public Task createBugTask(String title, Set<String> tags, int id, Severity severity) {
        return new BugTask(title, tags, id, severity);
    }

    public Task createFeatureTask(String title, Set<String> tags, int id, float estimatedTime) {
        return new FeatureTask(title, tags, id, estimatedTime);
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
        listTasks(new SortById());
    }

    // Sort a copy of the tasks with the given strategy, then print that sorted list
    public void listTasks(SortStrategy strategy) {
        List<Task> list = new ArrayList<>(tasks.values());
        strategy.sort(list);
        for (Task task : list) {
            System.out.println("ID: " + task.getId() + ", Title: " + task.getTitle()
                    + ", Status: " + task.getStatus() + ", Tags: " + task.getTags());
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


interface SortStrategy {
    void sort(List<Task> tasks);
}

class SortById implements SortStrategy {
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparingInt(Task::getId));
    }
}

class SortByTitle implements SortStrategy {
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparing(Task::getTitle));
    }
}

class SortByStatus implements SortStrategy {
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparing(Task::getStatus));   // PENDING before DONE
    }
}
