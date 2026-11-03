
import java.util.List;
import java.util.Set;

public class Main {

    public static void main(String[] args) {

        // the storage is chosen here; TaskManager only sees the interfaces
        UserRepository userRepo = new InMemoryUserRepository();
        ProjectRepository projectRepo = new InMemoryProjectRepository();
        TaskRepository taskRepo = new InMemoryTaskRepository(userRepo);
        TaskManager manager = new TaskManager(userRepo, projectRepo, taskRepo);

        // every task needs a project and a reporter
        User john = manager.createUser("John Doe", "john@example.com");
        User jane = manager.createUser("Jane Smith", "jane@example.com");
        Project project = manager.createProject("Personal", "PER", john.getId());
        manager.addMember(project.getId(), jane.getId(), Role.MEMBER);

        // the id passed to the constructor is ignored: the repository gives the real one
        TaskFactory factory = new TaskFactory();
        Task groceries = save(manager, new Task("Buy groceries", Set.of("shopping", "errands"), 0), project, john);
        Task finish = save(manager, new Task("Finish project", Set.of("work", "urgent"), 0), project, john);
        save(manager, new Task("Call mom", Set.of("family"), 0), project, john);
        Task bug = save(manager, factory.createTask("bug", "Fix login bug", Set.of("bug", "urgent"), 0, Severity.HIGH), project, john);
        save(manager, factory.createTask("feature", "Add dark mode", Set.of("feature"), 0, 3.5f), project, john);

        manager.addTag(groceries.getId(), "today");
        manager.assign(bug.getId(), jane.getId());

        manager.changeStatus(finish.getId(), Status.DONE, john.getId());
        System.out.println("Task with ID " + finish.getId() + " marked as done.");

        manager.addComment(bug.getId(), jane.getId(), "I can reproduce it on Safari");

        System.out.println("\n--- Sorted by ID ---");
        print(manager.listTasks(project.getId(), null, null, null, new SortById(), 1, 20));

        System.out.println("\n--- Sorted by title ---");
        print(manager.listTasks(project.getId(), null, null, null, new SortByTitle(), 1, 20));

        System.out.println("\n--- Sorted by status (pending first) ---");
        print(manager.listTasks(project.getId(), null, null, null, new SortByStatus(), 1, 20));

        System.out.println("\n--- Filter: pending tasks, sorted by title ---");
        print(manager.listTasks(project.getId(), Status.PENDING, null, null, new SortByTitle(), 1, 20));

        System.out.println("\n--- Filter: tagged 'urgent' ---");
        print(manager.listTasks(project.getId(), null, null, "urgent", new SortById(), 1, 20));

        System.out.println("\n--- Filter: urgent AND pending ---");
        print(manager.listTasks(project.getId(), Status.PENDING, null, "urgent", new SortById(), 1, 20));

        System.out.println("\n--- Filter: assigned to Jane ---");
        print(manager.listTasks(project.getId(), null, jane.getId(), null, new SortById(), 1, 20));

        System.out.println("\n--- Filter: tagged 'school' ---");
        print(manager.listTasks(project.getId(), null, null, "school", new SortById(), 1, 20));

        System.out.println("\n--- Page 2, 2 per page, sorted by ID ---");
        print(manager.listTasks(project.getId(), null, null, null, new SortById(), 2, 2));

        System.out.println("\n--- Task " + finish.getId() + " status history ---");
        for (StatusChange change : manager.findStatusHistory(finish.getId())) {
            System.out.println(change.getOldStatus() + " -> " + change.getNewStatus() + " by user " + change.getChangedBy());
        }

        System.out.println("\n--- Task " + bug.getId() + " comments ---");
        for (Comment comment : manager.findComments(bug.getId())) {
            System.out.println("User " + comment.getAuthorId() + ": " + comment.getBody());
        }
    }

    // fills in the project and reporter, then saves the task
    private static Task save(TaskManager manager, Task task, Project project, User reporter) {
        task.setProjectId(project.getId());
        task.setReporterId(reporter.getId());
        return manager.createTask(task);
    }

    private static void print(List<TaskListItem> items) {
        if (items.isEmpty()) {
            System.out.println("No tasks match this filter.");
            return;
        }
        for (TaskListItem item : items) {
            Task task = item.getTask();
            String assignee = item.getAssigneeName() == null ? "unassigned" : item.getAssigneeName();
            System.out.println("ID: " + task.getId() + ", Title: " + task.getTitle()
                    + ", Status: " + task.getStatus() + ", Assignee: " + assignee + ", Tags: " + task.getTags());
        }
    }
}
