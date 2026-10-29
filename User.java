import java.time.OffsetDateTime;

class User {
    private long id;
    private String fullName;
    private String email;
    private OffsetDateTime createdAt;

    public User(long id, String fullName, String email, OffsetDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
