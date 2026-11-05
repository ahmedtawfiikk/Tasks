import java.util.Set;

// Factory method: makes a BugTask or a FeatureTask from the type name,
// so the code that creates tasks doesn't need to know each subclass.
// extra is the severity for a bug, or the estimate in hours for a feature.
class TaskFactory {

    public Task createTask(String type, String title, Set<String> tags, long id, Object extra) {
        if (type.equalsIgnoreCase("bug")) {
            return new BugTask(title, tags, id, (Severity) extra);
        } else if (type.equalsIgnoreCase("feature")) {
            return new FeatureTask(title, tags, id, (Float) extra);
        } else {
            throw new IllegalArgumentException("Unknown task type: " + type);
        }
    }
}