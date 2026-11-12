import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps tasks, comments and status history in memory. Used when STORAGE=memory.
// Checks the same rules as the tasks, tags, comments and task_status_history tables.
class InMemoryTaskRepository implements TaskRepository {

    // id -> task. HashMap gives findById in O(1), as the spec requires.
    private final Map<Long, Task> tasks = new HashMap<>();

    // task id -> its comments / its status changes
    private final Map<Long, List<Comment>> comments = new HashMap<>();
    private final Map<Long, List<StatusChange>> history = new HashMap<>();

    // project id -> its tags, like the tags table: a tag belongs to one project,
    // and a name is used only once per project, ignoring case
    private final Map<Long, List<Tag>> projectTags = new HashMap<>();

    // the task list shows the assignee's name, and names live in the users
    private final UserRepository users;

    // tasks belong to projects, and an assignee must be a member of the task's project
    private final InMemoryProjectRepository projects;

    private long nextTaskId = 1;
    private long nextCommentId = 1;
    private long nextStatusChangeId = 1;
    private long nextTagId = 1;

    InMemoryTaskRepository(UserRepository users, InMemoryProjectRepository projects) {
        this.users = users;
        this.projects = projects;
    }

    @Override
    public Task create(Task task) {
        // the same rules as the tasks table
        if (projects.findById(task.getProjectId()) == null) {
            throw new IllegalArgumentException("No project with id " + task.getProjectId());
        }
        InMemoryChecks.notBlank(task.getTitle(), "Title");
        InMemoryChecks.userExists(users, task.getReporterId());
        checkAssignee(task.getProjectId(), task.getAssigneeId());
        if (task instanceof BugTask bug && bug.getSeverity() == null) {
            throw new IllegalArgumentException("A bug needs a severity");
        }
        if (task instanceof FeatureTask feature
                && (feature.getEstimatedTime() <= 0 || feature.getEstimatedTime() > 200)) {
            throw new IllegalArgumentException("Estimate must be more than 0 and at most 200 hours");
        }

        task.setId(nextTaskId++);   // the id passed in is ignored
        tasks.put(task.getId(), task);
        return task;
    }



    
    @Override
    public Task findById(long id) {
        return tasks.get(id);   // null if not found
    }

    @Override
    public void assign(long taskId, Long assigneeId) {
        Task task = getTask(taskId);
        checkAssignee(task.getProjectId(), assigneeId);
        task.setAssigneeId(assigneeId);   // null = unassign
    }

    // Hard rule 1, like the foreign key (project_id, assignee_id) -> project_members:
    // an assignee must be a member of the task's project. null (unassigned) is allowed.
    private void checkAssignee(long projectId, Long assigneeId) {
        if (assigneeId != null && !projects.isMember(projectId, assigneeId)) {
            throw new IllegalArgumentException("User " + assigneeId + " is not a member of project " + projectId);
        }
    }


    @Override
    public void changeStatus(long taskId, Status newStatus, long changedBy) {
        Task task = getTask(taskId);
        Status oldStatus = task.getStatus();
        if (oldStatus == newStatus) {
            throw new IllegalArgumentException("Task " + taskId + " is already " + newStatus);
        }
        InMemoryChecks.userExists(users, changedBy);

        // completed_at is set only when the status is DONE
        if (newStatus == Status.DONE) {
            task.complete();
        } else {
            task.setStatus(newStatus);
            task.setCompletedAt(null);
        }

        StatusChange change = new StatusChange(nextStatusChangeId++, taskId, oldStatus, newStatus,
                                               changedBy, OffsetDateTime.now());
        history.computeIfAbsent(taskId, id -> new ArrayList<>()).add(change);
    }

    

    @Override
    public void addTag(long taskId, String tagName) {
        Task task = getTask(taskId);
        Tag tag = findOrCreateTag(task.getProjectId(), tagName);
        task.addTag(tag.getName());   // the project's spelling; the Set ignores a tag the task already has
    }

    // All the checks come first, so if one fails the task is not changed at all.
    @Override
    public void moveTask(long taskId, long targetProjectId) {
        Task task = getTask(taskId);
        if (projects.findById(targetProjectId) == null) {
            throw new IllegalArgumentException("No project with id " + targetProjectId);
        }
        checkAssignee(targetProjectId, task.getAssigneeId());

        // the task keeps its tags by name: each one becomes the target project's tag
        List<String> newTagNames = new ArrayList<>();
        for (String tagName : task.getTags()) {
            newTagNames.add(findOrCreateTag(targetProjectId, tagName).getName());
        }

        task.setProjectId(targetProjectId);
        task.getTags().clear();
        task.getTags().addAll(newTagNames);
        // comments and history are kept by task id, so they stay with the task
    }

    // The project's tag with this name (ignoring case), or a new one, as in PostgreSQL:
    // if PAY has "backend", tagging a PAY task "Backend" uses "backend".
    private Tag findOrCreateTag(long projectId, String name) {
        InMemoryChecks.notBlank(name, "Tag name");
        List<Tag> tags = projectTags.computeIfAbsent(projectId, id -> new ArrayList<>());
        for (Tag tag : tags) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }
        Tag tag = new Tag(nextTagId++, projectId, name, OffsetDateTime.now());
        tags.add(tag);
        return tag;
    }



    
    @Override
    public Comment addComment(long taskId, long authorId, String body) {
        getTask(taskId);   // fails if the task doesn't exist
        InMemoryChecks.userExists(users, authorId);
        InMemoryChecks.notBlank(body, "Comment");
        Comment comment = new Comment(nextCommentId++, taskId, authorId, body, OffsetDateTime.now());
        comments.computeIfAbsent(taskId, id -> new ArrayList<>()).add(comment);
        return comment;
    }





    // Filter, then sort, then cut the page. The page must be last, after ALL the tasks
    // are filtered and sorted. The filters and sorts do the work; this only calls them in order.
    @Override
    public List<Task> findTasks(Long projectId, Status status, Long assigneeId, String tag,
                                SortStrategy sort, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize start at 1");
        }
        List<Task> all = new ArrayList<>(tasks.values());
        List<Task> matching = TaskFilter.of(projectId, status, assigneeId, tag).apply(all);
        SortStrategy order = sort != null ? sort : new SortById();   // no sort = id order
        order.sort(matching);
        return takePage(matching, page, pageSize);
    }



    // one page of the list, each task with its assignee's name
    private List<Task> takePage(List<Task> list, int page, int pageSize) {
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, list.size());
        List<Task> pageOfTasks = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Task task = list.get(i);
            task.setAssigneeName(assigneeName(task));
            pageOfTasks.add(task);
        }
        return pageOfTasks;
    }


    // the assignee's full name, or null when the task is unassigned
    private String assigneeName(Task task) {
        if (task.getAssigneeId() == null) {
            return null;
        }
        User assignee = users.findById(task.getAssigneeId());
        return assignee != null ? assignee.getFullName() : null;
    }

    @Override
    public List<Comment> findComments(long taskId) {
        return new ArrayList<>(comments.getOrDefault(taskId, Collections.emptyList()));
    }

    @Override
    public List<StatusChange> findStatusHistory(long taskId) {
        return new ArrayList<>(history.getOrDefault(taskId, Collections.emptyList()));
    }

    // finds the task or fails with a clear message
    private Task getTask(long taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("No task with id " + taskId);
        }
        return task;
    }
}
