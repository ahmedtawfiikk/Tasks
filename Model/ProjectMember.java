import java.time.OffsetDateTime;

enum Role {
    OWNER, MAINTAINER, MEMBER
}

class ProjectMember {
    private long projectId;
    private long userId;
    private Role role;
    private OffsetDateTime joinedAt;

    public ProjectMember(long projectId, long userId, Role role, OffsetDateTime joinedAt) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public long getProjectId() {
        return projectId;
    }

    public long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
