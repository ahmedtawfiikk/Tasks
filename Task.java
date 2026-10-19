
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class Task {
    private String title;
    private Status status = Status.PENDING;
    private int id;
    private final Set<String> tags = new HashSet<>();

    Task(String title, Set<String> tags, int id) {
        this.title = title;
        this.tags.addAll(tags);
        this.id = id;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public boolean addTag(String tag) {
        return tags.add(tag);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Status getStatus() {
        return status;
    }

    public int getId() {
        return id;
    }

    public void complete() {
        status = Status.DONE;
    }
}
