import java.util.Set;

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