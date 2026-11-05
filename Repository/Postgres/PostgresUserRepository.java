import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Stores users in PostgreSQL. Used when STORAGE=postgres.
class PostgresUserRepository implements UserRepository {
    private final Database db;
    private final RowMapper mapper;

    PostgresUserRepository(Database db, RowMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    @Override
    public User create(String fullName, String email) {
        return db.run(c -> {
            try (PreparedStatement ps = db.prepare(c, """
                    INSERT INTO users (full_name, email) VALUES (?, ?)
                    RETURNING id, full_name, email, created_at""")) {
                ps.setString(1, fullName);
                ps.setString(2, email);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return mapper.user(rs);
                }
            }
        });
    }

    @Override
    public User findById(long id) {
        return db.run(c -> {
            try (PreparedStatement ps = db.prepare(c,
                    "SELECT id, full_name, email, created_at FROM users WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? mapper.user(rs) : null;
                }
            }
        });
    }
}
