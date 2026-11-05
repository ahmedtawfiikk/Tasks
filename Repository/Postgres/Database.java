import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Opens connections to PostgreSQL and runs work on them.
// The settings come from the standard environment variables, never from a file in the repo.
class Database {

    // a piece of database work that may throw SQLException
    interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    private final String url;
    private final String user;
    private final String password;
    private final SqlLogger logger;

    Database(SqlLogger logger) {
        this.url = "jdbc:postgresql://" + setting("PGHOST") + ":" + setting("PGPORT") + "/" + setting("PGDATABASE");
        this.user = setting("PGUSER");
        this.password = setting("PGPASSWORD");
        this.logger = logger;
    }

    // every statement goes through here, so the debug flag sees all of them
    PreparedStatement prepare(Connection connection, String sql) throws SQLException {
        logger.log(sql);
        return connection.prepareStatement(sql);
    }

    // runs the work on a new connection with auto-commit: each statement is saved on its own
    <T> T run(Work<T> work) {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            return work.run(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // runs the work as one transaction: everything is saved, or nothing is
    <T> T inTransaction(Work<T> work) {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String setting(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set the environment variable " + name);
        }
        return value;
    }
}
