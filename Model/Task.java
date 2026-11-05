import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

enum Status {
    PENDING,
    IN_PROGRESS,
    DONE
}

class Task {
    private long id;
    private long projectId;
    private String title;
    private String description = "";
    private Status status = Status.PENDING;
    private long reporterId;
    private Long assigneeId;              // null = unassigned
    private String assigneeName;          // set by the task list; null = unassigned
    private LocalDate dueDate;            // null = no due date
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime completedAt;   // set only when status is DONE
    private Set<String> tags = new HashSet<>();

    Task(String title, Set<String> tags, long id) {
        this.title = title;
        this.tags.addAll(tags);
        this.id = id;
    }

    public Set<String> getTags() {
        return tags;
    }

    public boolean addTag(String tag) {
        return tags.add(tag);
    }

    public long getId() {
        return id;
    }

    // the repository gives the task its id when it is saved
    public void setId(long id) {
        this.id = id;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getReporterId() {
        return reporterId;
    }

    public void setReporterId(long reporterId) {
        this.reporterId = reporterId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void complete() {
        status = Status.DONE;
        completedAt = OffsetDateTime.now();
    }
}
