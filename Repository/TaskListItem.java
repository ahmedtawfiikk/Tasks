class TaskListItem {
    private Task task;
    private String assigneeName;   // null = unassigned

    public TaskListItem(Task task, String assigneeName) {
        this.task = task;
        this.assigneeName = assigneeName;
    }

    public Task getTask() {
        return task;
    }

    public String getAssigneeName() {
        return assigneeName;
    }
}
