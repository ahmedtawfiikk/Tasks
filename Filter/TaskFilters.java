// The filter rules. Each class is one rule, using the TaskFilter interface.

class FilterByProject implements TaskFilter {
    private long projectId;

    public FilterByProject(long projectId) {
        this.projectId = projectId;
    }

    public boolean matches(Task task) {
        return task.getProjectId() == projectId;
    }
}



class FilterByStatus implements TaskFilter {
    private Status status;

    public FilterByStatus(Status status) {
        this.status = status;
    }

    public boolean matches(Task task) {
        return task.getStatus() == status;
    }
}




class FilterByAssignee implements TaskFilter {
    private long assigneeId;

    public FilterByAssignee(long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public boolean matches(Task task) {
        // getAssigneeId() is null when the task is unassigned
        return task.getAssigneeId() != null && task.getAssigneeId() == assigneeId;
    }
}



// Tag names ignore case (docs/SCHEMA.md), so "Backend" matches "backend".
class FilterByTag implements TaskFilter {
    private String tag;

    public FilterByTag(String tag) {
        this.tag = tag;
    }

    public boolean matches(Task task) {
        for (String name : task.getTags()) {
            if (name.equalsIgnoreCase(tag)) {
                return true;
            }
        }
        return false;
    }
}



class FilterBySeverity implements TaskFilter {
    private Severity severity;

    public FilterBySeverity(Severity severity) {
        this.severity = severity;
    }

    public boolean matches(Task task) {
        return task instanceof BugTask && ((BugTask) task).getSeverity() == severity;
    }
}




class AndFilter implements TaskFilter {
    private TaskFilter first;
    private TaskFilter second;

    public AndFilter(TaskFilter first, TaskFilter second) {
        this.first = first;
        this.second = second;
    }

    public boolean matches(Task task) {
        return first.matches(task) && second.matches(task);
    }
}
