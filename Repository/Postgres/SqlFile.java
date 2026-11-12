import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Reads named SQL queries from a .sql file, so the SQL is written in one place only.
// In the file, a line "-- name: some_name" starts a query, and the query ends at the line
// that ends with ;. Comment lines and empty lines are skipped. Example:
//
//   -- name: overdue_tasks
//   -- a comment about how it works
//   SELECT ...
//   ORDER BY days_late DESC, t.id;
class SqlFile {
    private static final String NAME_LINE = "-- name:";

    // query name -> SQL text (without the ; at the end)
    private final Map<String, String> queries = new HashMap<>();
    private final Path path;

    SqlFile(Path path) {
        this.path = path;
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + path + ". Run the app from the project folder.", e);
        }

        String name = null;                       // the query we are reading now, or null between queries
        StringBuilder sql = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(NAME_LINE)) {
                // a new query starts
                name = trimmed.substring(NAME_LINE.length()).trim();
                sql = new StringBuilder();
            } else if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                // a comment or an empty line: not part of the SQL
            } else if (name != null) {
                sql.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    // the query is complete: save it without the ;
                    String text = sql.toString().trim();
                    queries.put(name, text.substring(0, text.length() - 1));
                    name = null;
                }
            }
        }
    }

    // the SQL of one query; fails clearly if the file has no query with that name
    String get(String name) {
        String sql = queries.get(name);
        if (sql == null) {
            throw new IllegalStateException("No query named '" + name + "' in " + path);
        }
        return sql;
    }
}
