import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

// The console: reads a command, calls TaskManager, prints the result.
// It never talks to a repository or the database directly.
class ConsoleApp {
    private final TaskManager manager;
    private final Scanner in;
    private final TaskFactory taskFactory = new TaskFactory();

    ConsoleApp(TaskManager manager, Scanner in) {
        this.manager = manager;
        this.in = in;
    }

    void run() {
        while (true) {
            printMenu();
            String choice = ask("Choose");
            if (choice == null || choice.equals("0")) {
                return;
            }
            try {
                runCommand(choice);
            } catch (RuntimeException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1  Create user");
        System.out.println("2  Create project");
        System.out.println("3  Add member to project");
        System.out.println("4  Create bug task");
        System.out.println("5  Create feature task");
        System.out.println("6  Assign task");
        System.out.println("7  Change task status");
        System.out.println("8  Tag task");
        System.out.println("9  Comment on task");
        System.out.println("10 List tasks");
        System.out.println("11 Show task");
        System.out.println("12 Move task to another project");
        System.out.println("13 Import tasks from CSV");
        System.out.println("0  Exit");
    }

    private void runCommand(String choice) {
        switch (choice) {
            case "1" -> createUser();
            case "2" -> createProject();
            case "3" -> addMember();
            case "4" -> createBug();
            case "5" -> createFeature();
            case "6" -> assign();
            case "7" -> changeStatus();
            case "8" -> tag();
            case "9" -> comment();
            case "10" -> listTasks();
            case "11" -> showTask();
            case "12" -> moveTask();
            case "13" -> importCsv();
            default -> System.out.println("Unknown choice: " + choice);
        }
    }

    private void createUser() {
        User user = manager.createUser(ask("Full name"), ask("Email"));
        System.out.println("Created user " + user.getId());
    }

    private void createProject() {
        Project project = manager.createProject(ask("Name"), ask("Key (2 to 5 capital letters)"),
                                                askLong("Owner user id"));
        System.out.println("Created project " + project.getKey() + " with id " + project.getId());
    }

    private void addMember() {
        long projectId = projectId(ask("Project key"));
        Role role = Role.valueOf(ask("Role (maintainer or member)").toUpperCase());
        manager.addMember(projectId, askLong("User id"), role);
        System.out.println("Member added");
    }

    private void createBug() {
        long projectId = projectId(ask("Project key"));
        String title = ask("Title");
        Severity severity = Severity.valueOf(ask("Severity (low, medium, high, critical)").toUpperCase());
        Task task = taskFactory.createTask("bug", title, new HashSet<>(), 0, severity);
        save(task, projectId);
    }

    private void createFeature() {
        long projectId = projectId(ask("Project key"));
        String title = ask("Title");
        float estimate = Float.parseFloat(ask("Estimate in hours (more than 0, at most 200)"));
        Task task = taskFactory.createTask("feature", title, new HashSet<>(), 0, estimate);
        save(task, projectId);
    }

    // asks for the fields every task has, then saves it
    private void save(Task task, long projectId) {
        task.setProjectId(projectId);
        task.setDescription(ask("Description (blank = none)"));
        task.setReporterId(askLong("Reporter user id"));
        String dueDate = ask("Due date YYYY-MM-DD (blank = none)");
        task.setDueDate(dueDate.isEmpty() ? null : LocalDate.parse(dueDate));
        Task saved = manager.createTask(task);
        System.out.println("Created task " + saved.getId());
    }

    private void assign() {
        long taskId = askLong("Task id");
        manager.assign(taskId, askOptionalLong("Assignee user id (blank = unassign)"));
        System.out.println("Done");
    }

    private void changeStatus() {
        long taskId = askLong("Task id");
        Status status = Status.valueOf(ask("New status (pending, in_progress, done)").toUpperCase());
        manager.changeStatus(taskId, status, askLong("Your user id"));
        System.out.println("Done");
    }

    private void tag() {
        manager.addTag(askLong("Task id"), ask("Tag"));
        System.out.println("Done");
    }

    private void moveTask() {
        long taskId = askLong("Task id");
        long targetProjectId = projectId(ask("Target project key"));
        manager.moveTask(taskId, targetProjectId);
        System.out.println("Moved task " + taskId);
    }

    // import tasks.csv PAY: every bad row is printed, or every task is saved
    private void importCsv() {
        String filePath = ask("CSV file");
        String projectKey = ask("Project key");
        long reporterId = askLong("Reporter user id");

        long start = System.currentTimeMillis();
        ImportResult result = manager.importCsv(filePath, projectKey, reporterId);
        long millis = System.currentTimeMillis() - start;

        if (!result.errors().isEmpty()) {
            System.out.println("Nothing was imported. " + result.errors().size() + " bad rows:");
            for (String error : result.errors()) {
                System.out.println("  " + error);
            }
            return;
        }
        System.out.println("Imported " + result.importedCount() + " tasks in " + millis + " ms");
    }

    private void comment() {
        long taskId = askLong("Task id");
        long authorId = askLong("Your user id");
        Comment comment = manager.addComment(taskId, authorId, ask("Comment"));
        System.out.println("Added comment " + comment.getId());
    }

    // Asks how to list first, then only the questions for that choice.
    // A filter that isn't asked stays null (= don't filter); a sort that isn't asked is by id.
    private void listTasks() {
        System.out.println("1  Default (all tasks, by id)");
        System.out.println("2  Filtered");
        System.out.println("3  Sorted");
        System.out.println("4  Filtered and sorted");
        String how = ask("How");
        if (!how.equals("1") && !how.equals("2") && !how.equals("3") && !how.equals("4")) {
            throw new IllegalArgumentException("Unknown choice: " + how);
        }
        boolean filtered = how.equals("2") || how.equals("4");
        boolean sorted = how.equals("3") || how.equals("4");

        Long projectId = null;
        Status status = null;
        Long assigneeId = null;
        String tag = null;
        if (filtered) {
            String key = ask("Project key (blank = all)");
            projectId = key.isEmpty() ? null : projectId(key);
            String statusText = ask("Status (blank = all)");
            status = statusText.isEmpty() ? null : Status.valueOf(statusText.toUpperCase());
            assigneeId = askOptionalLong("Assignee user id (blank = all)");
            String tagText = ask("Tag (blank = all)");
            tag = tagText.isEmpty() ? null : tagText;
        }
        SortStrategy sort = sorted ? askSort() : new SortById();
        int page = askInt("Page (blank = 1)", 1);
        int pageSize = askInt("Per page (blank = 20)", 20);

        printTasks(manager.listTasks(projectId, status, assigneeId, tag, sort, page, pageSize));
    }

    private void printTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No tasks match.");
        }
        for (Task task : tasks) {
            String assignee = task.getAssigneeName() != null ? task.getAssigneeName() : "unassigned";
            System.out.println("#" + task.getId() + "  " + task.getTitle() + "  [" + task.getStatus() + "]"
                    + "  assignee: " + assignee + "  tags: " + task.getTags());
        }
    }

    private void showTask() {
        long taskId = askLong("Task id");
        Task task = manager.findTaskById(taskId);
        if (task == null) {
            System.out.println("No task with id " + taskId);
            return;
        }
        System.out.println("#" + task.getId() + "  " + task.getTitle() + "  (" + details(task) + ")");
        System.out.println("Project: " + task.getProjectId() + "  Status: " + task.getStatus()
                + "  Reporter: " + task.getReporterId() + "  Assignee: " + orNone(task.getAssigneeId()));
        System.out.println("Due: " + orNone(task.getDueDate()) + "  Created: " + task.getCreatedAt()
                + "  Completed: " + orNone(task.getCompletedAt()));
        System.out.println("Description: " + task.getDescription());
        System.out.println("Tags: " + task.getTags());

        System.out.println("Comments:");
        for (Comment comment : manager.findComments(taskId)) {
            System.out.println("  " + comment.getCreatedAt() + "  user " + comment.getAuthorId() + ": " + comment.getBody());
        }
        System.out.println("Status history:");
        for (StatusChange change : manager.findStatusHistory(taskId)) {
            System.out.println("  " + change.getChangedAt() + "  " + change.getOldStatus() + " -> "
                    + change.getNewStatus() + " by user " + change.getChangedBy());
        }
    }

    private String details(Task task) {
        if (task instanceof BugTask bug) {
            return "bug, severity " + bug.getSeverity();
        }
        if (task instanceof FeatureTask feature) {
            return "feature, estimate " + feature.getEstimatedTime() + " h";
        }
        return "task";
    }

    // A sort is picked by its class name, so a new sort class works here
    // without changing this file.
    private SortStrategy askSort() {
        String name = ask("Sort (SortById, SortByTitle, SortByStatus; blank = SortById)");
        if (name.isEmpty()) {
            return new SortById();
        }
        try {
            return (SortStrategy) Class.forName(name).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalArgumentException("No sort called " + name);
        }
    }

    private long projectId(String key) {
        Project project = manager.findProjectByKey(key);
        if (project == null) {
            throw new IllegalArgumentException("No project with key " + key);
        }
        return project.getId();
    }

    // returns null when the input has ended
    private String ask(String label) {
        System.out.print(label + ": ");
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private long askLong(String label) {
        return Long.parseLong(ask(label));
    }

    private Long askOptionalLong(String label) {
        String text = ask(label);
        return text.isEmpty() ? null : Long.valueOf(text);
    }

    private int askInt(String label, int whenBlank) {
        String text = ask(label);
        return text.isEmpty() ? whenBlank : Integer.parseInt(text);
    }

    private String orNone(Object value) {
        return value != null ? value.toString() : "-";
    }
}
