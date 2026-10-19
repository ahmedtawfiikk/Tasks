
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

        TaskFactory factory = new TaskFactory();
        manager.addTask(factory.createTask("bug", "Fix login bug", Set.of("bug", "urgent"), 4, Severity.HIGH));
        manager.addTask(factory.createTask("feature", "Add dark mode", Set.of("feature"), 5, 3.5f));

        manager.markTaskAsDone(2);

        System.out.println("\n--- Sorted by ID ---");
        manager.listTasks(new SortById());

        System.out.println("\n--- Sorted by title ---");
        manager.listTasks(new SortByTitle());

        System.out.println("\n--- Sorted by status (pending first) ---");
        manager.listTasks(new SortByStatus());
    
    }
}




















