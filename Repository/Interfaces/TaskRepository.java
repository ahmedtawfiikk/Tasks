import java.util.List;

interface TaskRepository {

    // saves the task without tags (tags are added with addTag).
    // The id of the given task is ignored; returns the saved task with its new id.
    Task create(Task task);

    // returns null if there is no task with this id; the task comes with its tags
    Task findById(long id);

    // assigneeId null = unassign
    void assign(long taskId, Long assigneeId);

    void changeStatus(long taskId, Status newStatus, long changedBy);

    void addTag(long taskId, String tagName);

    // Moves the task to another project, all or nothing.
    // The task keeps its tags by name: each one becomes the target project's tag with that name.
    // Fails if the assignee is not a member of the target project.
    // Comments and status history stay with the task.
    void moveTask(long taskId, long targetProjectId);

    Comment addComment(long taskId, long authorId, String body);

    // every filter argument can be null, meaning "don't filter by this"; page starts at 1.
    // Each task comes with its tags and its assignee name.
    List<Task> findTasks(Long projectId, Status status, Long assigneeId, String tag,
                                 SortStrategy sort, int page, int pageSize);

    List<Comment> findComments(long taskId);

    List<StatusChange> findStatusHistory(long taskId);
}
