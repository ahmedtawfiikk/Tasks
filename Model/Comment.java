import java.time.OffsetDateTime;

class Comment {
    private long id;
    private long taskId;
    private long authorId;
    private String body;
    private OffsetDateTime createdAt;

    public Comment(long id, long taskId, long authorId, String body, OffsetDateTime createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getTaskId() {
        return taskId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
