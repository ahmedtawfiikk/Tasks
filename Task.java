
import java.util.HashSet;
import java.util.Set;



enum Status {
    PENDING,
    DONE
}

class Task {
    private String title;
    private Status status = Status.PENDING;   
    private int id;
    private Set<String> tags = new HashSet<>();

    Task(String title, Set<String> tags, int id) {
        this.title = title;
        this.tags.addAll(tags);
        this.id = id;
    }



   

    protected Set<String> getTags() {
        return tags;
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
