import java.util.ArrayList;
import java.util.List;

// Strategy pattern: each filter is one rule that says yes or no to a task.
// The in-memory repository uses these filters to list tasks.
// PostgreSQL doesn't use them: it filters in the WHERE of its query, so it never
// loads tasks that don't match.
interface TaskFilter {
    boolean matches(Task task);

    // Builds one filter from the task list's values: starts with a filter that accepts
    // every task, then adds one filter for each value that was given (null = don't filter by this).
    static TaskFilter of(Long projectId, Status status, Long assigneeId, String tag) {
        TaskFilter filter = task -> true;
        if (projectId != null) {
            filter = new AndFilter(filter, new FilterByProject(projectId));
        }
        if (status != null) {
            filter = new AndFilter(filter, new FilterByStatus(status));
        }
        if (assigneeId != null) {
            filter = new AndFilter(filter, new FilterByAssignee(assigneeId));
        }
        if (tag != null) {
            filter = new AndFilter(filter, new FilterByTag(tag));
        }
        return filter;
    }


    
    // the tasks this filter accepts, in the same order
    default List<Task> apply(List<Task> tasks) {
        List<Task> matching = new ArrayList<>();
        for (Task task : tasks) {
            if (matches(task)) {
                matching.add(task);
            }
        }
        return matching;
    }
}
