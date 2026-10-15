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

        Taskfactory taskfactory = new Taskfactory();
       taskfactory.createBugTask("Fix login bug", Set.of("bug", "urgent"), 4, severity.HIGH);
       taskfactory.createFeatureTask("Add dark mode", Set.of("feature"), 5, 3.5f);
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





interface task {
    void createtask();

}


enum severity {
    LOW, MEDIUM, HIGH
}

class BugTask extends Task {
    private severity severity;

    

    public BugTask(String title, Set<String> tags, int id, severity severity) {
        super(title, tags, id);
        this.severity = severity;
    }



    public severity getSeverity() {
        return severity;
    }

    

}


class FeatureTask extends Task {
    float estimatedTime = 0.0f;

    public FeatureTask(String title, Set<String> tags, int id, float estimatedTime) {
        super(title, tags, id);
        this.estimatedTime = estimatedTime;
    }

    public float getEstimatedTime() {
        return estimatedTime;
    }
    
    

}

 class Taskfactory  {
    
    public Task createBugTask(String title, Set<String> tags, int id, severity severity) {
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


    public void sortTasksbyID() {
        
    }
}
