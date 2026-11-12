import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

// Writes a valid CSV file with many tasks, to time the import (Part 3.3: 10,000 rows in under 10 s).
//   java -cp out TasksCsvGenerator samples/tasks_10k.csv 10000
// The assignees are the PAY members from db/seed.sql, so import the file into PAY.
public class TasksCsvGenerator {

    private static final String[] ASSIGNEES = {
        "john@example.com", "jane@example.com", "alice@example.com", "mohamed@example.com", "ziad@example.com", ""
    };
    private static final String[] SEVERITIES = {"low", "medium", "high", "critical"};
    private static final String[] TAGS = {"", "backend", "frontend|api", "backend|urgent", "reports|bulk"};

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args[0]);
        int count = Integer.parseInt(args[1]);

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            out.println("type,title,description,severity,estimate_hours,assignee_email,due_date,tags");
            for (int i = 1; i <= count; i++) {
                boolean bug = i % 2 == 0;
                String type = bug ? "bug" : "feature";
                String severity = bug ? SEVERITIES[i % SEVERITIES.length] : "";
                String estimate = bug ? "" : String.valueOf(1 + i % 200);
                String assignee = ASSIGNEES[i % ASSIGNEES.length];
                String dueDate = i % 3 == 0 ? "2027-0" + (1 + i % 9) + "-15" : "";
                String tags = TAGS[i % TAGS.length];
                // every title is different, so there are no duplicate titles
                out.println(type + ",Imported task " + i + ",Generated row " + i + ","
                        + severity + "," + estimate + "," + assignee + "," + dueDate + "," + tags);
            }
        }
        System.out.println("Wrote " + count + " rows to " + file);
    }
}
