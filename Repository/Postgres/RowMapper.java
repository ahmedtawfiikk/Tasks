import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;

// Turns database rows into objects, and task objects into column values.
// Database enum values are lowercase ('in_progress'); Java enum values are uppercase (IN_PROGRESS).
class RowMapper {
    private final TaskFactory taskFactory = new TaskFactory();

    User user(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"),
                        rs.getObject("created_at", OffsetDateTime.class));
    }

    Project project(ResultSet rs) throws SQLException {
        return new Project(rs.getLong("id"), rs.getString("name"), rs.getString("key"),
                           rs.getLong("owner_id"), rs.getObject("created_at", OffsetDateTime.class));
    }

    Comment comment(ResultSet rs) throws SQLException {
        return new Comment(rs.getLong("id"), rs.getLong("task_id"), rs.getLong("author_id"),
                           rs.getString("body"), rs.getObject("created_at", OffsetDateTime.class));
    }

    StatusChange statusChange(ResultSet rs) throws SQLException {
        return new StatusChange(rs.getLong("id"), rs.getLong("task_id"),
                                status(rs.getString("old_status")), status(rs.getString("new_status")),
                                rs.getLong("changed_by"), rs.getObject("changed_at", OffsetDateTime.class));
    }

    // TaskFactory decides BugTask or FeatureTask from the type column.
    Task task(ResultSet rs) throws SQLException {
        // the database makes sure a bug has a severity and a feature has an estimate
        // extra is the bug's severity or the feature's estimate, as TaskFactory expects
        String severity = rs.getString("severity");
        Object extra;
        if (severity != null) {
            extra = Severity.valueOf(severity.toUpperCase());
        } else {
            extra = rs.getFloat("estimate_hours");
        }
        Task task = taskFactory.createTask(rs.getString("type"), rs.getString("title"),
                                           new HashSet<>(), rs.getLong("id"), extra);
        task.setProjectId(rs.getLong("project_id"));
        task.setDescription(rs.getString("description"));
        task.setStatus(status(rs.getString("status")));
        task.setReporterId(rs.getLong("reporter_id"));
        task.setAssigneeId(rs.getObject("assignee_id", Long.class));
        task.setDueDate(rs.getObject("due_date", LocalDate.class));
        task.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        task.setCompletedAt(rs.getObject("completed_at", OffsetDateTime.class));
        return task;
    }

    // the type column of a task object
    String typeOf(Task task) {
        if (task instanceof BugTask) {
            return "bug";
        }
        if (task instanceof FeatureTask) {
            return "feature";
        }
        throw new IllegalArgumentException("Only bug and feature tasks can be saved");
    }

    // the severity column: only bugs have one
    String severityOf(Task task) {
        return task instanceof BugTask bug ? text(bug.getSeverity()) : null;
    }

    // the estimate_hours column: only features have one
    BigDecimal estimateOf(Task task) {
        return task instanceof FeatureTask feature ? BigDecimal.valueOf(feature.getEstimatedTime()) : null;
    }

    // IN_PROGRESS -> "in_progress"
    String text(Enum<?> value) {
        return value.name().toLowerCase();
    }

    // "in_progress" -> IN_PROGRESS
    Status status(String text) {
        return Status.valueOf(text.toUpperCase());
    }
}
