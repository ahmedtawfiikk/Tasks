import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





class TaskManager {
    private Map<Integer, Task> tasks = new HashMap<>();

    public void addTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public Task findTaskById(int id) {
        return tasks.get(id);
    }

    public void listTasks() {
        listTasks(new SortById());
    }

    public void listTasks(SortStrategy strategy) {
        listTasks(task -> true, strategy);
    }

    public void listTasks(TaskFilter filter, SortStrategy strategy) {
        List<Task> list = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (filter.matches(task)) {
                list.add(task);
            }
        }
        if (list.isEmpty()) {
            System.out.println("No tasks match this filter.");
            return;
        }
        strategy.sort(list);
        for (Task task : list) {
            System.out.println("ID: " + task.getId() + ", Title: " + task.getTitle()
                    + ", Status: " + task.getStatus() + ", Tags: " + task.getTags());
        }
    }

    public void markTaskAsDone(int id) {
        Task task = findTaskById(id);
        if (task != null) {
            task.complete();
            System.out.println("Task with ID " + id + " marked as done.");
        } else {
            System.out.println("Task with ID " + id + " not found.");
        }
    }
}