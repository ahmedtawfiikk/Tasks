import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The database work of the CSV import (Part 3.3). Used when STORAGE=postgres.
// The number of statements is fixed: it doesn't grow with the number of rows in the file.
class PostgresImportRepository implements ImportRepository {
    private final Database db;
    private final RowMapper mapper;

    PostgresImportRepository(Database db, RowMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    // one query for all the emails of the file
    @Override
    public Map<String, Long> findUserIdsByEmail(Collection<String> emails) {
        return db.run(c -> {
            Map<String, Long> userIds = new HashMap<>();
            try (PreparedStatement ps = db.prepare(c,
                    "SELECT id, email FROM users WHERE lower(email) = ANY (?)")) {
                ps.setArray(1, c.createArrayOf("text", emails.toArray()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        userIds.put(rs.getString("email").toLowerCase(), rs.getLong("id"));
                    }
                }
            }
            return userIds;
        });
    }

    // one query for all the members of the project
    @Override
    public Set<Long> findMemberIds(long projectId) {
        return db.run(c -> {
            Set<Long> memberIds = new HashSet<>();
            try (PreparedStatement ps = db.prepare(c,
                    "SELECT user_id FROM project_members WHERE project_id = ?")) {
                ps.setLong(1, projectId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        memberIds.add(rs.getLong("user_id"));
                    }
                }
            }
            return memberIds;
        });
    }

    // One transaction with JDBC batching. Each step sends all its rows in one batch:
    // 1. add the tags the project doesn't have yet   2. add the tasks   3. link tasks and tags
    // If anything fails (or the app is killed), nothing is saved.
    @Override
    public void saveTasks(long projectId, List<Task> tasks) {
        db.inTransaction(c -> {
            insertTags(c, projectId, tasks);
            insertTasks(c, tasks);
            linkTags(c, projectId, tasks);
            return null;
        });
    }

    // adds every tag name of the file once, in one batch.
    // A tag the project already has is skipped by the database: the unique index
    // on (project_id, lower(name)) makes it a conflict, and ON CONFLICT DO NOTHING skips it.
    private void insertTags(Connection c, long projectId, List<Task> tasks) throws SQLException {
        try (PreparedStatement ps = db.prepare(c,
                "INSERT INTO tags (project_id, name) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            Set<String> added = new HashSet<>(); // lower-case names already in the batch
            for (Task task : tasks) {
                for (String tagName : task.getTags()) {
                    if (added.add(tagName.toLowerCase())) { // true only the first time
                        ps.setLong(1, projectId);
                        ps.setString(2, tagName);
                        ps.addBatch();
                    }
                }
            }
            ps.executeBatch();
        }
    }

    // inserts all the tasks in one batch, then gives each task its new id
    private void insertTasks(Connection c, List<Task> tasks) throws SQLException {
        try (PreparedStatement ps = db.prepareReturningId(c, """
                INSERT INTO tasks (project_id, type, title, description, reporter_id,
                                   assignee_id, due_date, severity, estimate_hours)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
            for (Task task : tasks) {
                ps.setLong(1, task.getProjectId());
                ps.setObject(2, mapper.typeOf(task), Types.OTHER);
                ps.setString(3, task.getTitle());
                ps.setString(4, task.getDescription());
                ps.setLong(5, task.getReporterId());
                ps.setObject(6, task.getAssigneeId(), Types.BIGINT);
                ps.setObject(7, task.getDueDate(), Types.DATE);
                ps.setObject(8, mapper.severityOf(task), Types.OTHER);
                ps.setBigDecimal(9, mapper.estimateOf(task));
                ps.addBatch();
            }
            ps.executeBatch();
            // the new ids come back in the same order as the batch
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int i = 0;
                while (keys.next()) {
                    tasks.get(i).setId(keys.getLong("id"));
                    i++;
                }
            }
        }
    }

    // links every task to its tags, in one batch.
    // We send the tag name; the database finds the tag's id in the project.
    private void linkTags(Connection c, long projectId, List<Task> tasks) throws SQLException {
        try (PreparedStatement ps = db.prepare(c, """
                INSERT INTO task_tags (task_id, tag_id, project_id)
                SELECT ?, id, project_id FROM tags
                WHERE project_id = ? AND lower(name) = lower(?)""")) {
            for (Task task : tasks) {
                for (String tagName : task.getTags()) {
                    ps.setLong(1, task.getId());
                    ps.setLong(2, projectId);
                    ps.setString(3, tagName);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }
}
