// One row of report 2: a task that is not done and whose due date has passed.
// assignee is "Unassigned" when the task has no assignee.
record OverdueTask(String projectKey, String title, String assignee, int daysLate) {
}
