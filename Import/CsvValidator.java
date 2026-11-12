import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Checks every CSV row before anything is written, and turns the good rows into tasks.
// It never stops at the first bad row: every problem of every row is collected with its line number.
// It needs no database: the users and members were looked up once, before the check.
class CsvValidator {
    private final long projectId;
    private final long reporterId;
    private final Map<String, Long> userIdsByEmail;   // lower-case email -> user id
    private final Set<Long> memberIds;                // members of the project
    private final TaskFactory taskFactory = new TaskFactory();

    // trimmed, lower-case title -> the line it was first seen on, to find duplicate titles
    private final Map<String, Integer> titleLines = new HashMap<>();

    private final List<String> errors = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();

    CsvValidator(long projectId, long reporterId, Map<String, Long> userIdsByEmail, Set<Long> memberIds) {
        this.projectId = projectId;
        this.reporterId = reporterId;
        this.userIdsByEmail = userIdsByEmail;
        this.memberIds = memberIds;
    }

    List<String> getErrors() {
        return errors;
    }

    List<Task> getTasks() {
        return tasks;
    }

    void check(CsvRow row) {
        if (row.columnCount() != CsvReader.COLUMNS) {
            errors.add("Line " + row.lineNumber() + ": has " + row.columnCount()
                    + " columns, expected " + CsvReader.COLUMNS);
            return;
        }

        List<String> problems = new ArrayList<>();
        String type = checkType(row, problems);
        String title = checkTitle(row, problems);
        Severity severity = checkSeverity(row, type, problems);
        Float estimate = checkEstimate(row, type, problems);
        Long assigneeId = checkAssignee(row, problems);
        LocalDate dueDate = checkDueDate(row, problems);
        Set<String> tags = readTags(row);

        if (!problems.isEmpty()) {
            errors.add("Line " + row.lineNumber() + ": " + String.join("; ", problems));
            return;
        }

        // TaskFactory makes the BugTask or FeatureTask, as everywhere else
        Object extra = type.equals("bug") ? severity : estimate;
        Task task = taskFactory.createTask(type, title, tags, 0, extra);
        task.setProjectId(projectId);
        task.setDescription(row.description());
        task.setReporterId(reporterId);
        task.setAssigneeId(assigneeId);
        task.setDueDate(dueDate);
        tasks.add(task);
    }

    // "bug" or "feature"; null when the type is wrong
    private String checkType(CsvRow row, List<String> problems) {
        String type = row.type().toLowerCase();
        if (!type.equals("bug") && !type.equals("feature")) {
            problems.add("wrong type '" + row.type() + "' (use bug or feature)");
            return null;
        }
        return type;
    }

    // the title must not be empty or appear twice in the file (trimmed, ignoring case)
    private String checkTitle(CsvRow row, List<String> problems) {
        String title = row.title();
        if (title.isEmpty()) {
            problems.add("title is empty");
            return title;
        }
        Integer firstLine = titleLines.putIfAbsent(title.toLowerCase(), row.lineNumber());
        if (firstLine != null) {
            problems.add("duplicate title '" + title + "' (also on line " + firstLine + ")");
        }
        return title;
    }

    // a bug needs a severity; a feature has none
    private Severity checkSeverity(CsvRow row, String type, List<String> problems) {
        String text = row.severity();
        if ("feature".equals(type)) {
            if (!text.isEmpty()) {
                problems.add("a feature has no severity");
            }
            return null;
        }
        if (!"bug".equals(type)) {
            return null;   // the type is already wrong
        }
        if (text.isEmpty()) {
            problems.add("a bug needs a severity");
            return null;
        }
        try {
            return Severity.valueOf(text.toUpperCase());
        } catch (IllegalArgumentException e) {
            problems.add("wrong severity '" + text + "' (use low, medium, high or critical)");
            return null;
        }
    }

    // a feature needs an estimate, more than 0 and at most 200 hours; a bug has none
    private Float checkEstimate(CsvRow row, String type, List<String> problems) {
        String text = row.estimateHours();
        if ("bug".equals(type)) {
            if (!text.isEmpty()) {
                problems.add("a bug has no estimate");
            }
            return null;
        }
        if (!"feature".equals(type)) {
            return null;   // the type is already wrong
        }
        if (text.isEmpty()) {
            problems.add("a feature needs an estimate");
            return null;
        }
        float estimate;
        try {
            estimate = Float.parseFloat(text);
        } catch (NumberFormatException e) {
            problems.add("estimate '" + text + "' is not a number");
            return null;
        }
        if (estimate <= 0 || estimate > 200) {
            problems.add("estimate " + text + " is out of range (more than 0, at most 200)");
            return null;
        }
        return estimate;
    }

    // empty = unassigned; otherwise the user must exist and be a member of the project
    private Long checkAssignee(CsvRow row, List<String> problems) {
        String email = row.assigneeEmail();
        if (email.isEmpty()) {
            return null;
        }
        Long userId = userIdsByEmail.get(email.toLowerCase());
        if (userId == null) {
            problems.add("unknown assignee " + email);
            return null;
        }
        if (!memberIds.contains(userId)) {
            problems.add("assignee " + email + " is not a member of the project");
            return null;
        }
        return userId;
    }

    // empty = no due date; otherwise YYYY-MM-DD
    private LocalDate checkDueDate(CsvRow row, List<String> problems) {
        String text = row.dueDate();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            problems.add("bad due date '" + text + "' (use YYYY-MM-DD)");
            return null;
        }
    }

    // tags are separated by |. A tag repeated in one row (api|API|api) counts once.
    private Set<String> readTags(CsvRow row) {
        Set<String> tags = new HashSet<>();
        Set<String> seen = new HashSet<>();   // lower-case names already taken
        for (String part : row.tags().split("\\|")) {
            String tag = part.trim();
            if (!tag.isEmpty() && seen.add(tag.toLowerCase())) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
