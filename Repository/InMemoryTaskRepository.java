import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps tasks, comments and status history in memory. Used when STORAGE=memory.
class InMemoryTaskRepository implements TaskRepository {

    // id -> task. HashMap gives findById in O(1), as the spec requires.
    private final Map<Long, Task> tasks = new HashMap<>();

    // task id -> its comments / its status changes
    private final Map<Long, List<Comment>> comments = new HashMap<>();
    private final Map<Long, List<StatusChange>> history = new HashMap<>();

    // the task list shows the assignee's name, and names live in the users
    private final UserRepository users;

    private long nextTaskId = 1;
    private long nextCommentId = 1;
    private long nextStatusChangeId = 1;

    InMemoryTaskRepository(UserRepository users) {
        this.users = users;
    }

    @Override
    public Task create(Task task) {
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
        getTask(taskId).setAssigneeId(assigneeId);   // null = unassign
    }

    @Override
    public void changeStatus(long taskId, Status newStatus, long changedBy) {
        Task task = getTask(taskId);
        Status oldStatus = task.getStatus();
        if (oldStatus == newStatus) {
            throw new IllegalArgumentException("Task " + taskId + " is already " + newStatus);
        }

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
        getTask(taskId).addTag(tagName);
    }

    @Override
    public Comment addComment(long taskId, long authorId, String body) {
        getTask(taskId);   // fails if the task doesn't exist
        Comment comment = new Comment(nextCommentId++, taskId, authorId, body, OffsetDateTime.now());
        comments.computeIfAbsent(taskId, id -> new ArrayList<>()).add(comment);
        return comment;
    }

    @Override
    public List<TaskListItem> findTasks(Long projectId, Status status, Long assigneeId, String tag,
                                        SortStrategy sort, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize start at 1");
        }

        // 1. filter: a null filter means "don't filter by this"
        List<Task> matching = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (projectId != null && task.getProjectId() != projectId) continue;
            if (status != null && task.getStatus() != status) continue;
            if (assigneeId != null && !assigneeId.equals(task.getAssigneeId())) continue;
            if (tag != null && !task.getTags().contains(tag)) continue;
            matching.add(task);
        }

        // 2. sort: put them in id order first. List.sort keeps equal items in their order,
        //    so two tasks with the same title stay in id order and never swap between pages.
        matching.sort(Comparator.comparingLong(Task::getId));
        if (sort != null) {
            sort.sort(matching);
        }

        // 3. take one page, and add each task's assignee name
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, matching.size());
        List<TaskListItem> items = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Task task = matching.get(i);
            String assigneeName = null;   // null = unassigned
            if (task.getAssigneeId() != null) {
                User assignee = users.findById(task.getAssigneeId());
                if (assignee != null) {
                    assigneeName = assignee.getFullName();
                }
            }
            items.add(new TaskListItem(task, assigneeName));
        }
        return items;
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
