import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// The five reports of Part 4, each one SQL query. Used when STORAGE=postgres.
// The SQL is not written here: it comes from db/reports.sql (through SqlFile),
// where each query has a comment on how it works. This class runs a query and turns the rows into records.
// The queries only read, so they run with db.run (no transaction needed).
class PostgresReportRepository implements ReportRepository {
    private final Database db;
    private final SqlFile sql;

    PostgresReportRepository(Database db, SqlFile sql) {
        this.db = db;
        this.sql = sql;
    }

    // 1. Project progress
    @Override
    public List<ProjectProgress> projectProgress() {
        return db.run(c -> {
            List<ProjectProgress> rows = new ArrayList<>();
            try (PreparedStatement ps = db.prepare(c, sql.get("project_progress"));
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ProjectProgress(rs.getString("key"), rs.getString("name"),
                            rs.getLong("total"), rs.getLong("done"), rs.getLong("in_progress"),
                            rs.getLong("pending"), rs.getBigDecimal("percent_done")));
                }
            }
            return rows;
        });
    }

    // 2. Overdue tasks
    @Override
    public List<OverdueTask> overdueTasks() {
        return db.run(c -> {
            List<OverdueTask> rows = new ArrayList<>();
            try (PreparedStatement ps = db.prepare(c, sql.get("overdue_tasks"));
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OverdueTask(rs.getString("key"), rs.getString("title"),
                            rs.getString("assignee"), rs.getInt("days_late")));
                }
            }
            return rows;
        });
    }

    // 3. Top 3 people per project
    @Override
    public List<TopPerson> topPeople() {
        return db.run(c -> {
            List<TopPerson> rows = new ArrayList<>();
            try (PreparedStatement ps = db.prepare(c, sql.get("top_people"));
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TopPerson(rs.getString("key"), rs.getString("full_name"),
                            rs.getLong("done_tasks")));
                }
            }
            return rows;
        });
    }

    // 4a. Members with no tasks, with NOT EXISTS
    @Override
    public List<MemberWithoutTasks> membersWithoutTasksNotExists() {
        return db.run(c -> members(c, sql.get("members_without_tasks_not_exists")));
    }

    // 4b. Members with no tasks, with LEFT JOIN ... IS NULL
    @Override
    public List<MemberWithoutTasks> membersWithoutTasksLeftJoin() {
        return db.run(c -> members(c, sql.get("members_without_tasks_left_join")));
    }

    // 4c. Members with no tasks, with NOT IN: wrong on purpose (see db/reports.sql)
    @Override
    public List<MemberWithoutTasks> membersWithoutTasksNotIn() {
        return db.run(c -> members(c, sql.get("members_without_tasks_not_in")));
    }

    // the three versions of report 4 give the same columns, so they share this
    private List<MemberWithoutTasks> members(Connection c, String query) throws SQLException {
        List<MemberWithoutTasks> rows = new ArrayList<>();
        try (PreparedStatement ps = db.prepare(c, query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new MemberWithoutTasks(rs.getString("key"), rs.getString("full_name")));
            }
        }
        return rows;
    }

    // 5. Tasks with all given tags
    @Override
    public List<TaskTitle> tasksWithAllTags(long projectId, List<String> tagNames) {
        // lower case and no repeats: the query compares COUNT(*) with the size of the list,
        // so "backend, Backend" must count as one tag
        Set<String> names = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            if (!tagName.isBlank()) {
                names.add(tagName.trim().toLowerCase());
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException("Give at least one tag");
        }

        return db.run(c -> {
            List<TaskTitle> rows = new ArrayList<>();
            try (PreparedStatement ps = db.prepare(c, sql.get("tasks_with_all_tags"))) {
                ps.setLong(1, projectId);
                ps.setArray(2, c.createArrayOf("text", names.toArray()));
                ps.setInt(3, names.size());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new TaskTitle(rs.getLong("id"), rs.getString("title")));
                    }
                }
            }
            return rows;
        });
    }
}
