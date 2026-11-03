import java.util.List;

// The business logic. It only knows the repository interfaces, never how they store data,
// so the same TaskManager works with memory or PostgreSQL. It returns data and never prints.
class TaskManager {
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;

    TaskManager(UserRepository users, ProjectRepository projects, TaskRepository tasks) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
    }

    public User createUser(String fullName, String email) {
        return users.create(fullName, email);
    }

    public Project createProject(String name, String key, long ownerId) {
        return projects.create(name, key, ownerId);
    }

    public void addMember(long projectId, long userId, Role role) {
        projects.addMember(projectId, userId, role);
    }

    // returns the saved task with its new id
    public Task createTask(Task task) {
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

    public Comment addComment(long taskId, long authorId, String body) {
        return tasks.addComment(taskId, authorId, body);
    }

    // every filter can be null, meaning "don't filter by this"; page starts at 1
    public List<TaskListItem> listTasks(Long projectId, Status status, Long assigneeId, String tag,
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
