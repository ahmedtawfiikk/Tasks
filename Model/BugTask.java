import java.util.Set;

enum Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}

class BugTask extends Task {
    private Severity severity;

    public BugTask(String title, Set<String> tags, long id, Severity severity) {
        super(title, tags, id);
        this.severity = severity;
    }

    public Severity getSeverity() {
        return severity;
    }
}
