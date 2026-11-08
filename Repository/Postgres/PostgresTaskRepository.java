import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Stores tasks, tags, comments and status history in PostgreSQL. Used when STORAGE=postgres.
class PostgresTaskRepository implements TaskRepository {
    private final Database db;
    private final RowMapper mapper;

    PostgresTaskRepository(Database db, RowMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    // Saves a new task, without tags (tags are added later with addTag).
    // A new task is always pending, so the database fills these columns itself:
    // id, status ('pending'), completed_at (NULL) and created_at (now).
    @Override
    public Task create(Task task) {
        return db.run(c -> {
            try (PreparedStatement ps = db.prepare(c, """
                    INSERT INTO tasks (project_id, type, title, description, reporter_id,
                                       assignee_id, due_date, severity, estimate_hours)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id, created_at""")) {
                ps.setLong(1, task.getProjectId());
                // type and severity are database enums. Types.OTHER sends the text without
                // saying "this is text", so PostgreSQL reads it as the column's enum.
                ps.setObject(2, mapper.typeOf(task), Types.OTHER);       // "bug" or "feature"
                ps.setString(3, task.getTitle());
                ps.setString(4, task.getDescription());
                ps.setLong(5, task.getReporterId());
                ps.setObject(6, task.getAssigneeId(), Types.BIGINT);     // null = unassigned
                ps.setObject(7, task.getDueDate(), Types.DATE);          // null = no due date
                ps.setObject(8, mapper.severityOf(task), Types.OTHER);   // null for a feature
                ps.setBigDecimal(9, mapper.estimateOf(task));            // null for a bug
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    task.setId(rs.getLong("id"));   // the id passed in is ignored
                    task.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
                }
            }
            return task;
        });
    }

    // 2 queries: the task, then its tags
    @Override
    public Task findById(long id) {
        return db.run(c -> {
            Task task;
            try (PreparedStatement ps = db.prepare(c, """
                    SELECT id, project_id, type, title, description, status, reporter_id, assignee_id,
                           due_date, severity, estimate_hours, created_at, completed_at
                    FROM tasks
                    WHERE id = ?""")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    task = mapper.task(rs);
                }
            }
            loadTags(c, List.of(task));
            return task;
        });
    }

    @Override
    public void assign(long taskId, Long assigneeId) {
        db.run(c -> {
            try (PreparedStatement ps = db.prepare(c, "UPDATE tasks SET assignee_id = ? WHERE id = ?")) {
                ps.setObject(1, assigneeId, Types.BIGINT);   // null = unassign
                ps.setLong(2, taskId);
                if (ps.executeUpdate() == 0) {
                    throw new IllegalArgumentException("No task with id " + taskId);
                }
            }
            return null;
        });
    }

    // one transaction: the status, completed_at and the history row change together
    @Override
    public void changeStatus(long taskId, Status newStatus, long changedBy) {
        db.inTransaction(c -> {
            Status oldStatus = findStatus(c, taskId);
            if (oldStatus == newStatus) {
                throw new IllegalArgumentException("Task " + taskId + " is already " + newStatus);
            }
            updateStatus(c, taskId, newStatus);
            addStatusHistory(c, taskId, oldStatus, newStatus, changedBy);
            return null;
        });
    }

    // the task's current status, or an error if there is no such task
    private Status findStatus(Connection c, long taskId) throws SQLException {
        try (PreparedStatement ps = db.prepare(c, "SELECT status FROM tasks WHERE id = ?")) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No task with id " + taskId);
                }
                return mapper.status(rs.getString("status"));
            }
        }
    }

    // done: completed_at = now(); any other status: completed_at = NULL
    private void updateStatus(Connection c, long taskId, Status newStatus) throws SQLException {
        String sql;
        if (newStatus == Status.DONE) {
            sql = "UPDATE tasks SET status = ?, completed_at = now() WHERE id = ?";
        } else {
            sql = "UPDATE tasks SET status = ?, completed_at = NULL WHERE id = ?";
        }
        try (PreparedStatement ps = db.prepare(c, sql)) {
            ps.setObject(1, mapper.text(newStatus), Types.OTHER);
            ps.setLong(2, taskId);
            ps.executeUpdate();
        }
    }

    // one row in the history: who changed the task from which status to which
    private void addStatusHistory(Connection c, long taskId, Status oldStatus, Status newStatus,
                                  long changedBy) throws SQLException {
        try (PreparedStatement ps = db.prepare(c, """
                INSERT INTO task_status_history (task_id, old_status, new_status, changed_by)
                VALUES (?, ?, ?, ?)""")) {
            ps.setLong(1, taskId);
            ps.setObject(2, mapper.text(oldStatus), Types.OTHER);
            ps.setObject(3, mapper.text(newStatus), Types.OTHER);
            ps.setLong(4, changedBy);
            ps.executeUpdate();
        }
    }

    // one transaction: the tag (if new) and the link to the task are saved together
    @Override
    public void addTag(long taskId, String tagName) {
        db.inTransaction(c -> {
            long projectId;
            try (PreparedStatement ps = db.prepare(c, "SELECT project_id FROM tasks WHERE id = ?")) {
                ps.setLong(1, taskId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("No task with id " + taskId);
                    }
                    projectId = rs.getLong("project_id");
                }
            }
            saveTag(c, taskId, projectId, tagName);
            return null;
        });
    }

    @Override
    public Comment addComment(long taskId, long authorId, String body) {
        return db.run(c -> {
            try (PreparedStatement ps = db.prepare(c, """
                    INSERT INTO comments (task_id, author_id, body) VALUES (?, ?, ?)
                    RETURNING id, task_id, author_id, body, created_at""")) {
                ps.setLong(1, taskId);
                ps.setLong(2, authorId);
                ps.setString(3, body);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return mapper.comment(rs);
                }
            }
        });
    }

    // The task list. The database filters, sorts and cuts the page, so Java only gets one page.
    // 2 queries, however many tasks are on the page: 1. the tasks   2. their tags
    @Override
    public List<Task> findTasks(Long projectId, Status status, Long assigneeId, String tag,
                                SortStrategy sort, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize start at 1");
        }

        // 1. Start with every task and its assignee's name.
        //    LEFT JOIN keeps the tasks that have no assignee.
        //    "WHERE 1 = 1" is always true, so every filter below can simply start with AND.
        StringBuilder sql = new StringBuilder("""
                SELECT t.id, t.project_id, t.type, t.title, t.description, t.status, t.reporter_id,
                       t.assignee_id, t.due_date, t.severity, t.estimate_hours, t.created_at, t.completed_at,
                       u.full_name AS assignee_name
                FROM tasks t
                LEFT JOIN users u ON u.id = t.assignee_id
                WHERE 1 = 1""");

        // the values of the ? marks, in the same order as the ? marks
        List<Object> values = new ArrayList<>();

        // 2. Add only the filters the user chose. The SQL text is fixed; the values go in ? marks.
        if (projectId != null) {
            sql.append(" AND t.project_id = ?");
            values.add(projectId);
        }
        if (status != null) {
            sql.append(" AND t.status = CAST(? AS task_status)");
            values.add(mapper.text(status));
        }
        if (assigneeId != null) {
            sql.append(" AND t.assignee_id = ?");
            values.add(assigneeId);
        }
        if (tag != null) {
            // the tasks that have a tag with this name, ignoring case
            sql.append(" AND t.id IN (SELECT tt.task_id FROM task_tags tt"
                     + " JOIN tags g ON g.id = tt.tag_id WHERE lower(g.name) = lower(?))");
            values.add(tag);
        }

        // 3. Sort. A ? can't be a column name, so the column comes from the SortColumn enum,
        //    never from the user. t.id at the end keeps tasks with the same title in id order,
        //    so they never swap between pages.
        SortStrategy order = sort != null ? sort : new SortById();
        sql.append(" ORDER BY ").append(order.column().sql());
        if (order.descending()) {
            sql.append(" DESC");
        }
        sql.append(", t.id");

        // 4. Cut the page: page 3 with 20 per page skips 40 tasks and takes 20.
        sql.append(" LIMIT ? OFFSET ?");
        values.add(pageSize);
        values.add((page - 1) * pageSize);

        return db.run(c -> {
            List<Task> tasks = new ArrayList<>();
            try (PreparedStatement ps = db.prepare(c, sql.toString())) {
                for (int i = 0; i < values.size(); i++) {
                    ps.setObject(i + 1, values.get(i));   // the ? marks are numbered from 1
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Task task = mapper.task(rs);
                        task.setAssigneeName(rs.getString("assignee_name"));
                        tasks.add(task);
                    }
                }
            }
            loadTags(c, tasks);
            return tasks;
        });
    }

    @Override
    public List<Comment> findComments(long taskId) {
        return db.run(c -> {
            List<Comment> comments = new ArrayList<>();
            try (PreparedStatement ps = db.prepare(c, """
                    SELECT id, task_id, author_id, body, created_at
                    FROM comments
                    WHERE task_id = ?
                    ORDER BY created_at, id""")) {
                ps.setLong(1, taskId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        comments.add(mapper.comment(rs));
                    }
                }
            }
            return comments;
        });
    }

    @Override
    public List<StatusChange> findStatusHistory(long taskId) {
        return db.run(c -> {
            List<StatusChange> history = new ArrayList<>();
            try (PreparedStatement ps = db.prepare(c, """
                    SELECT id, task_id, old_status, new_status, changed_by, changed_at
                    FROM task_status_history
                    WHERE task_id = ?
                    ORDER BY changed_at, id""")) {
                ps.setLong(1, taskId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        history.add(mapper.statusChange(rs));
                    }
                }
            }
            return history;
        });
    }

    // Loads the tags of all the given tasks with ONE query, not one query per task (N+1).
    private void loadTags(Connection c, List<Task> tasks) throws SQLException {
        if (tasks.isEmpty()) {
            return;
        }

        // the tasks by id, so each tag row finds its task at once, with no loop inside a loop
        Map<Long, Task> tasksById = new HashMap<>();
        for (Task task : tasks) {
            tasksById.put(task.getId(), task);
        }

        try (PreparedStatement ps = db.prepare(c, """
                SELECT tt.task_id, g.name
                FROM task_tags tt
                JOIN tags g ON g.id = tt.tag_id
                WHERE tt.task_id = ANY (?)""")) {
            // all the task ids go in the one ? as an array
            ps.setArray(1, c.createArrayOf("bigint", tasksById.keySet().toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task task = tasksById.get(rs.getLong("task_id"));
                    task.addTag(rs.getString("name"));
                }
            }
        }
    }

    // Gives the task a tag. A tag belongs to a project, and its name is used once per project,
    // ignoring case: if PAY has "backend", tagging a PAY task "Backend" uses "backend".
    private void saveTag(Connection c, long taskId, long projectId, String tagName) throws SQLException {
        Long tagId = findTagId(c, projectId, tagName);
        if (tagId == null) {
            tagId = createTag(c, projectId, tagName);
        }

        // link the tag to the task; if the task already has it, do nothing
        try (PreparedStatement ps = db.prepare(c, """
                INSERT INTO task_tags (task_id, tag_id, project_id) VALUES (?, ?, ?)
                ON CONFLICT DO NOTHING""")) {
            ps.setLong(1, taskId);
            ps.setLong(2, tagId);
            ps.setLong(3, projectId);
            ps.executeUpdate();
        }
    }

    // the id of the project's tag with this name (ignoring case), or null if there is none
    private Long findTagId(Connection c, long projectId, String tagName) throws SQLException {
        try (PreparedStatement ps = db.prepare(c,
                "SELECT id FROM tags WHERE project_id = ? AND lower(name) = lower(?)")) {
            ps.setLong(1, projectId);
            ps.setString(2, tagName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    // creates a new tag in the project and returns its id
    private long createTag(Connection c, long projectId, String tagName) throws SQLException {
        try (PreparedStatement ps = db.prepare(c,
                "INSERT INTO tags (project_id, name) VALUES (?, ?) RETURNING id")) {
            ps.setLong(1, projectId);
            ps.setString(2, tagName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }
}
