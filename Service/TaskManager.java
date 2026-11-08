import java.util.List;

// The business logic. It only knows the repository interfaces, never how they store data,
// so the same TaskManager works with memory or PostgreSQL. It returns data and never prints.
class TaskManager {
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final CsvImporter csvImporter;

    TaskManager(UserRepository users, ProjectRepository projects, TaskRepository tasks,
                ImportRepository imports) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.csvImporter = new CsvImporter(users, projects, imports);
    }

    // loads every task of the CSV file into the project, all or nothing
    public ImportResult importCsv(String filePath, String projectKey, long reporterId) {
        return csvImporter.importFile(filePath, projectKey, reporterId);
    }

    public User createUser(String fullName, String email) {
        return users.create(fullName, email);
    }

    public Project createProject(String name, String key, long ownerId) {
        return projects.create(name, key, ownerId);
    }

    // returns null if there is no project with this key
    public Project findProjectByKey(String key) {
        return projects.findByKey(key);
    }

    public void addMember(long projectId, long userId, Role role) {
        projects.addMember(projectId, userId, role);
    }

    // returns the saved task with its new id.
    // A task is created without tags; tags are added after, with addTag.
    public Task createTask(Task task) {
        if (!task.getTags().isEmpty()) {
            throw new IllegalArgumentException("Create the task first, then add its tags with addTag");
        }
        return tasks.create(task);
    }

    // returns null if there is no task with this id
    public Task findTaskById(long id) {
        return tasks.findById(id);
    }

    // assigneeId null = unassign
    public void assign(long taskId, Long assigneeId) {
        tasks.assign(taskId, assigneeId);
    }

    public void changeStatus(long taskId, Status newStatus, long changedBy) {
        tasks.changeStatus(taskId, newStatus, changedBy);
    }

    public void addTag(long taskId, String tagName) {
        tasks.addTag(taskId, tagName);
    }

    public void moveTask(long taskId, long targetProjectId) {
        tasks.moveTask(taskId, targetProjectId);
    }

    public Comment addComment(long taskId, long authorId, String body) {
        return tasks.addComment(taskId, authorId, body);
    }

    // every filter can be null, meaning "don't filter by this"; page starts at 1
    public List<Task> listTasks(Long projectId, Status status, Long assigneeId, String tag,
                                SortStrategy sort, int page, int pageSize) {
        return tasks.findTasks(projectId, status, assigneeId, tag, sort, page, pageSize);
    }

    public List<Comment> findComments(long taskId) {
        return tasks.findComments(taskId);
    }

    public List<StatusChange> findStatusHistory(long taskId) {
        return tasks.findStatusHistory(taskId);
    }
}
