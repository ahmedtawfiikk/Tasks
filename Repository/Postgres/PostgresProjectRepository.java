import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;

// Stores projects and members in PostgreSQL. Used when STORAGE=postgres.
// projects has no owner column: the owner is the project_members row with role 'owner'.
class PostgresProjectRepository implements ProjectRepository {
    private final Database db;
    private final RowMapper mapper;

    PostgresProjectRepository(Database db, RowMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    // one transaction: a project is never saved without its owner
    @Override
    public Project create(String name, String key, long ownerId) {
        return db.inTransaction(c -> {
            long id;
            OffsetDateTime createdAt;
            try (PreparedStatement ps = db.prepare(c,
                    "INSERT INTO projects (name, key) VALUES (?, ?) RETURNING id, created_at")) {
                ps.setString(1, name);
                ps.setString(2, key);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    id = rs.getLong("id");
                    createdAt = rs.getObject("created_at", OffsetDateTime.class);
                }
            }
            try (PreparedStatement ps = db.prepare(c,
                    "INSERT INTO project_members (project_id, user_id, role) VALUES (?, ?, 'owner')")) {
                ps.setLong(1, id);
                ps.setLong(2, ownerId);
                ps.executeUpdate();
            }
            return new Project(id, name, key, ownerId, createdAt);
        });
    }

    @Override
    public Project findById(long id) {
        return db.run(c -> {
            try (PreparedStatement ps = db.prepare(c, """
                    SELECT p.id, p.name, p.key, p.created_at, m.user_id AS owner_id
                    FROM projects p
                    JOIN project_members m ON m.project_id = p.id AND m.role = 'owner'
                    WHERE p.id = ?""")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapper.project(rs) : null;
                }
            }
        });
    }

    @Override
    public Project findByKey(String key) {
        return db.run(c -> {
            try (PreparedStatement ps = db.prepare(c, """
                    SELECT p.id, p.name, p.key, p.created_at, m.user_id AS owner_id
                    FROM projects p
                    JOIN project_members m ON m.project_id = p.id AND m.role = 'owner'
                    WHERE p.key = ?""")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapper.project(rs) : null;
                }
            }
        });
    }

    @Override
    public void addMember(long projectId, long userId, Role role) {
        if (role == Role.OWNER) {
            throw new IllegalArgumentException("The owner is set only when the project is created");
        }
        db.run(c -> {
            try (PreparedStatement ps = db.prepare(c, """
                    INSERT INTO project_members (project_id, user_id, role)
                    VALUES (?, ?, CAST(? AS member_role))""")) {
                ps.setLong(1, projectId);
                ps.setLong(2, userId);
                ps.setString(3, mapper.text(role));
                ps.executeUpdate();
            }
            return null;
        });
    }
}
