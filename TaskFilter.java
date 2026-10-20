interface TaskFilter {
    boolean matches(Task task);
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

class FilterByTag implements TaskFilter {
    private String tag;

    public FilterByTag(String tag) {
        this.tag = tag;
    }

    public boolean matches(Task task) {
        return task.getTags().contains(tag);
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
