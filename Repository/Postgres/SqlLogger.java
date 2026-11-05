// The SQL debug flag: with SQL_DEBUG=true, every SQL statement the app runs is printed,
// so you can count the queries of a command.
class SqlLogger {
    private final boolean enabled;

    SqlLogger(boolean enabled) {
        this.enabled = enabled;
    }

    void log(String sql) {
        if (enabled) {
            System.out.println("[SQL] " + sql);
        }
    }
}
