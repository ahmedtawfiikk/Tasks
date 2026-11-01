import java.time.OffsetDateTime;

class Project {
    private long id;
    private String name;
    private String key;
    private long ownerId;
    private OffsetDateTime createdAt;

    public Project(long id, String name, String key, long ownerId, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.key = key;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
