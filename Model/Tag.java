import java.time.OffsetDateTime;

class Tag {
    private long id;
    private long projectId;
    private String name;
    private OffsetDateTime createdAt;

    public Tag(long id, long projectId, String name, OffsetDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
