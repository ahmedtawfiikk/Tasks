import java.time.OffsetDateTime;

class StatusChange {
    private long id;
    private long taskId;
    private Status oldStatus;
    private Status newStatus;
    private long changedBy;
    private OffsetDateTime changedAt;

    public StatusChange(long id, long taskId, Status oldStatus, Status newStatus, long changedBy, OffsetDateTime changedAt) {
        this.id = id;
        this.taskId = taskId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public long getId() {
        return id;
    }

    public long getTaskId() {
        return taskId;
    }

    public Status getOldStatus() {
        return oldStatus;
    }

    public Status getNewStatus() {
        return newStatus;
    }

    public long getChangedBy() {
        return changedBy;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }
}
