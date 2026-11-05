import java.util.Comparator;

// The sort orders. Each class is one order, using the SortStrategy interface.

class SortById implements SortStrategy {
    public Comparator<Task> comparator() {
        return Comparator.comparingLong(Task::getId);
    }

    public SortColumn column() {
        return SortColumn.ID;
    }
}

class SortByTitle implements SortStrategy {
    public Comparator<Task> comparator() {
        return Comparator.comparing(Task::getTitle);
    }

    public SortColumn column() {
        return SortColumn.TITLE;
    }
}

// pending, in_progress, done: the Java enum and the PostgreSQL enum are declared
// in the same order, so both storages sort statuses the same way
class SortByStatus implements SortStrategy {
    public Comparator<Task> comparator() {
        return Comparator.comparing(Task::getStatus);
    }

    public SortColumn column() {
        return SortColumn.STATUS;
    }
}
