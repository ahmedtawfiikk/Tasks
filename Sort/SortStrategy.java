import java.util.Comparator;
import java.util.List;

// A sort order that works in both storages:
//  - in memory, the repository sorts with comparator()
//  - in PostgreSQL, the repository puts column() into ORDER BY
// Adding a sort = adding one class. TaskManager and the repositories don't change.
interface SortStrategy {

    Comparator<Task> comparator();

    SortColumn column();

    // smallest first by default; a sort like "newest first" returns true
    default boolean descending() {
        return false;
    }

    // Sorts the list in memory: by id first, then by this strategy.
    // List.sort keeps equal items in their order, so two tasks with the same title
    // stay in id order and never swap between pages (like "ORDER BY ..., t.id" in SQL).
    default void sort(List<Task> tasks) {
        tasks.sort(Comparator.comparingLong(Task::getId));
        tasks.sort(descending() ? comparator().reversed() : comparator());
    }
}
