import java.util.Comparator;
import java.util.List;




    interface SortStrategy {
    void sort(List<Task> tasks);
}

class SortById implements SortStrategy {
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparingInt(Task::getId));
    }
}

class SortByTitle implements SortStrategy {
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparing(Task::getTitle));
    }
}

class SortByStatus implements SortStrategy {
    public void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparing(Task::getStatus));   
    }
}

