import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Part 5: fills a separate database with a lot of realistic, skewed data.
//   $env:PGDATABASE = "taskmanager_big"
//   java -cp "lib/*;out" DataGenerator generate --users 2000 --projects 50 --tasks 500000
// Run it on an empty database that has all the migrations.
//
// The data:
// - some projects are much bigger than others: project number i gets a share of 1/(i+1)
// - about 70% of the tasks are done (15% in progress, 15% pending)
// - a history row for every status change: a done task went pending -> in_progress -> done
// - 0 to 5 tags per task, from its own project's tags
//
// Every insert is a JDBC batch. The tasks are saved in chunks of 10,000,
// each chunk (its tasks, their tags and their history) in one transaction.
// Members and tags are kept in memory, so no task ever needs a query to pick its assignee or tags.
class DataGenerator {

    private static final int CHUNK = 10_000;

    private static final String[] TAG_NAMES = {
        "backend", "frontend", "api", "urgent", "ui", "database", "security", "performance", "docs", "testing",
        "mobile", "payments", "auth", "infra", "reports", "search", "email", "refactor", "design", "analytics"
    };
    private static final String[] SEVERITIES = {"low", "medium", "high", "critical"};

    private final Database db;
    private final Random random = new Random();
    private final OffsetDateTime now = OffsetDateTime.now();

    // filled step by step, then used to make the tasks
    private long[] userIds;
    private long[] projectIds;
    private long[][] memberIds;        // memberIds[p] = the user ids of the members of project p
    private long[][] tagIds;           // tagIds[p]    = the tag ids of project p
    private double[] projectShares;    // running total of the project shares, to pick a project

    private long taskTagCount;
    private long historyCount;

    DataGenerator(Database db) {
        this.db = db;
    }

    public static void main(String[] args) {
        int users = argument(args, "--users");
        int projects = argument(args, "--projects");
        int tasks = argument(args, "--tasks");

        DataGenerator generator = new DataGenerator(new Database(new SqlLogger(false)));
        long start = System.nanoTime();
        generator.generate(users, projects, tasks);
        long seconds = (System.nanoTime() - start) / 1_000_000_000;

        System.out.println("Generated " + users + " users, " + projects + " projects, " + tasks + " tasks, "
                + generator.taskTagCount + " task tags and " + generator.historyCount + " history rows in "
                + seconds + " s");
    }

    // the number after a flag such as --users; stops with a clear message if it is missing
    private static int argument(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return Integer.parseInt(args[i + 1]);
            }
        }
        throw new IllegalArgumentException("Missing " + flag
                + ". Use: generate --users 2000 --projects 50 --tasks 500000");
    }

    void generate(int users, int projects, int tasks) {
        db.inTransaction(c -> {
            insertUsers(c, users);
            insertProjects(c, projects);
            insertMembers(c);
            insertTags(c);
            return null;
        });
        System.out.println("Users, projects, members and tags are saved");

        projectShares = shares(projects);
        for (int done = 0; done < tasks; done += CHUNK) {
            int size = Math.min(CHUNK, tasks - done);
            List<GeneratedTask> chunk = makeTasks(size, done);
            db.inTransaction(c -> {
                insertTasks(c, chunk);
                insertTaskTags(c, chunk);
                insertHistory(c, chunk);
                return null;
            });
            System.out.println("Tasks saved: " + (done + size));
        }
    }

    // ---------- users, projects, members, tags ----------

    private void insertUsers(Connection c, int count) throws SQLException {
        try (PreparedStatement ps = db.prepareReturningId(c, "INSERT INTO users (full_name, email) VALUES (?, ?)")) {
            for (int i = 1; i <= count; i++) {
                ps.setString(1, "User " + i);
                ps.setString(2, "user" + i + "@example.com");
                ps.addBatch();
            }
            ps.executeBatch();
            userIds = readIds(ps, count);
        }
    }

    private void insertProjects(Connection c, int count) throws SQLException {
        try (PreparedStatement ps = db.prepareReturningId(c, "INSERT INTO projects (name, key) VALUES (?, ?)")) {
            for (int i = 0; i < count; i++) {
                ps.setString(1, "Project " + (i + 1));
                ps.setString(2, projectKey(i));
                ps.addBatch();
            }
            ps.executeBatch();
            projectIds = readIds(ps, count);
        }
    }

    // a key the schema accepts (2 to 5 capital letters): P then 3 letters, PAAA, PAAB, ...
    private static String projectKey(int index) {
        char[] letters = new char[3];
        for (int i = 2; i >= 0; i--) {
            letters[i] = (char) ('A' + index % 26);
            index /= 26;
        }
        return "P" + new String(letters);
    }

    // each project gets 10 to 30 different users; the first one is its only owner
    private void insertMembers(Connection c) throws SQLException {
        memberIds = new long[projectIds.length][];
        try (PreparedStatement ps = db.prepare(c,
                "INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, ?)")) {
            for (int p = 0; p < projectIds.length; p++) {
                int count = Math.min(userIds.length, 10 + random.nextInt(21));
                memberIds[p] = pickDifferent(userIds, count);
                for (int m = 0; m < memberIds[p].length; m++) {
                    ps.setLong(1, projectIds[p]);
                    ps.setLong(2, memberIds[p][m]);
                    ps.setObject(3, m == 0 ? "owner" : "member", Types.OTHER);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    // every project gets the same 20 tag names (each project has its own tags)
    private void insertTags(Connection c) throws SQLException {
        try (PreparedStatement ps = db.prepareReturningId(c, "INSERT INTO tags (project_id, name) VALUES (?, ?)")) {
            for (long projectId : projectIds) {
                for (String name : TAG_NAMES) {
                    ps.setLong(1, projectId);
                    ps.setString(2, name);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            long[] ids = readIds(ps, projectIds.length * TAG_NAMES.length);
            tagIds = new long[projectIds.length][];
            for (int p = 0; p < projectIds.length; p++) {
                tagIds[p] = Arrays.copyOfRange(ids, p * TAG_NAMES.length, (p + 1) * TAG_NAMES.length);
            }
        }
    }

    // ---------- the tasks ----------

    // one task before it is saved; id is set after the insert
    private static class GeneratedTask {
        long id;
        int project;                   // index into projectIds
        String type;
        String title;
        String status;
        long reporterId;
        Long assigneeId;               // null = unassigned
        LocalDate dueDate;             // null = no due date
        String severity;               // bugs only
        BigDecimal estimate;           // features only
        OffsetDateTime createdAt;
        OffsetDateTime startedAt;      // when it went to in_progress (in progress and done tasks)
        OffsetDateTime completedAt;    // done tasks only
        long[] tags;
    }

    // Project i gets a share of 1/(i+1): project 1 gets the most tasks, project 50 about 50 times fewer.
    // The array holds the running total, so a random number between 0 and the total picks a project.
    private static double[] shares(int projects) {
        double[] total = new double[projects];
        double sum = 0;
        for (int i = 0; i < projects; i++) {
            sum += 1.0 / (i + 1);
            total[i] = sum;
        }
        return total;
    }

    private int pickProject() {
        double r = random.nextDouble() * projectShares[projectShares.length - 1];
        int p = Arrays.binarySearch(projectShares, r);
        return p >= 0 ? p : -p - 1;   // not found: binarySearch gives -(where it would go) - 1
    }

    private List<GeneratedTask> makeTasks(int count, int alreadyMade) {
        List<GeneratedTask> tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            GeneratedTask t = new GeneratedTask();
            int number = alreadyMade + i + 1;
            t.project = pickProject();
            long[] members = memberIds[t.project];

            boolean bug = random.nextBoolean();
            t.type = bug ? "bug" : "feature";
            t.title = (bug ? "Fix problem " : "Add feature ") + number;
            if (bug) {
                t.severity = SEVERITIES[random.nextInt(SEVERITIES.length)];
            } else {
                t.estimate = BigDecimal.valueOf(1 + random.nextInt(200));   // 1 to 200 hours
            }

            // the reporter and the assignee are members of the project; 1 task in 10 has no assignee
            t.reporterId = members[random.nextInt(members.length)];
            t.assigneeId = random.nextInt(10) == 0 ? null : members[random.nextInt(members.length)];

            // created in the last 2 years; most tasks have a due date 1 to 90 days after that
            t.createdAt = now.minusMinutes(random.nextInt(2 * 365 * 24 * 60));
            if (random.nextInt(10) < 7) {
                t.dueDate = t.createdAt.toLocalDate().plusDays(1 + random.nextInt(90));
            }

            // about 70% done, 15% in progress, 15% pending
            int r = random.nextInt(100);
            t.status = r < 70 ? "done" : r < 85 ? "in_progress" : "pending";
            if (!t.status.equals("pending")) {
                t.startedAt = between(t.createdAt, now);
            }
            if (t.status.equals("done")) {
                t.completedAt = between(t.startedAt, now);
            }

            t.tags = pickDifferent(tagIds[t.project], random.nextInt(6));   // 0 to 5 tags
            tasks.add(t);
        }
        return tasks;
    }

    // a random moment from 'from' to 'to'
    private OffsetDateTime between(OffsetDateTime from, OffsetDateTime to) {
        long seconds = java.time.Duration.between(from, to).getSeconds();
        return from.plusSeconds(seconds <= 0 ? 0 : (long) (random.nextDouble() * seconds));
    }

    private void insertTasks(Connection c, List<GeneratedTask> tasks) throws SQLException {
        try (PreparedStatement ps = db.prepareReturningId(c, """
                INSERT INTO tasks (project_id, type, title, status, reporter_id, assignee_id,
                                   due_date, severity, estimate_hours, created_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
            for (GeneratedTask t : tasks) {
                ps.setLong(1, projectIds[t.project]);
                ps.setObject(2, t.type, Types.OTHER);
                ps.setString(3, t.title);
                ps.setObject(4, t.status, Types.OTHER);
                ps.setLong(5, t.reporterId);
                ps.setObject(6, t.assigneeId, Types.BIGINT);
                ps.setObject(7, t.dueDate, Types.DATE);
                ps.setObject(8, t.severity, Types.OTHER);
                ps.setBigDecimal(9, t.estimate);
                ps.setObject(10, t.createdAt);
                ps.setObject(11, t.completedAt);
                ps.addBatch();
            }
            ps.executeBatch();
            long[] ids = readIds(ps, tasks.size());
            for (int i = 0; i < tasks.size(); i++) {
                tasks.get(i).id = ids[i];
            }
        }
    }

    private void insertTaskTags(Connection c, List<GeneratedTask> tasks) throws SQLException {
        try (PreparedStatement ps = db.prepare(c,
                "INSERT INTO task_tags (task_id, tag_id, project_id) VALUES (?, ?, ?)")) {
            for (GeneratedTask t : tasks) {
                for (long tagId : t.tags) {
                    ps.setLong(1, t.id);
                    ps.setLong(2, tagId);
                    ps.setLong(3, projectIds[t.project]);
                    ps.addBatch();
                    taskTagCount++;
                }
            }
            ps.executeBatch();
        }
    }

    // one row per status change: pending -> in_progress, then in_progress -> done
    private void insertHistory(Connection c, List<GeneratedTask> tasks) throws SQLException {
        try (PreparedStatement ps = db.prepare(c, """
                INSERT INTO task_status_history (task_id, old_status, new_status, changed_at, changed_by)
                VALUES (?, ?, ?, ?, ?)""")) {
            for (GeneratedTask t : tasks) {
                // the assignee made the change; an unassigned task was changed by its reporter
                long changedBy = t.assigneeId != null ? t.assigneeId : t.reporterId;
                if (t.startedAt != null) {
                    addHistory(ps, t.id, "pending", "in_progress", t.startedAt, changedBy);
                }
                if (t.completedAt != null) {
                    addHistory(ps, t.id, "in_progress", "done", t.completedAt, changedBy);
                }
            }
            ps.executeBatch();
        }
    }

    private void addHistory(PreparedStatement ps, long taskId, String from, String to,
                            OffsetDateTime at, long changedBy) throws SQLException {
        ps.setLong(1, taskId);
        ps.setObject(2, from, Types.OTHER);
        ps.setObject(3, to, Types.OTHER);
        ps.setObject(4, at);
        ps.setLong(5, changedBy);
        ps.addBatch();
        historyCount++;
    }

    // ---------- helpers ----------

    // the ids of the rows a batch inserted, in the order they were added
    private static long[] readIds(PreparedStatement ps, int count) throws SQLException {
        long[] ids = new long[count];
        try (ResultSet keys = ps.getGeneratedKeys()) {
            int i = 0;
            while (keys.next()) {
                ids[i++] = keys.getLong("id");
            }
        }
        return ids;
    }

    // 'count' different values from 'from', in random order
    private long[] pickDifferent(long[] from, int count) {
        Set<Long> picked = new LinkedHashSet<>();
        while (picked.size() < count) {
            picked.add(from[random.nextInt(from.length)]);
        }
        long[] result = new long[count];
        int i = 0;
        for (long value : picked) {
            result[i++] = value;
        }
        return result;
    }
}
