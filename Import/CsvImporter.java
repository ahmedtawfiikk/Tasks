import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The CSV import (Part 3.3), all or nothing:
// 1. read the file   2. look up the users, the project and its members once, for the whole file
// 3. check every row   4. if any row is bad, save nothing; otherwise save all the tasks.
// It only knows the repository interfaces, like TaskManager.
// In memory mode the first import lookup stops with "CSV import works only with STORAGE=postgres".
class CsvImporter {
    private final UserRepository users;
    private final ProjectRepository projects;
    private final ImportRepository imports;
    private final CsvReader reader = new CsvReader();

    CsvImporter(UserRepository users, ProjectRepository projects, ImportRepository imports) {
        this.users = users;
        this.projects = projects;
        this.imports = imports;
    }

    ImportResult importFile(String filePath, String projectKey, long reporterId) {
        // 1. read the file
        List<CsvRow> rows = reader.read(filePath);

        // 2. a fixed number of queries for the whole file, not one per row
        Set<String> emails = new HashSet<>();
        for (CsvRow row : rows) {
            if (!row.assigneeEmail().isEmpty()) {
                emails.add(row.assigneeEmail().toLowerCase());
            }
        }
        Map<String, Long> userIds = imports.findUserIdsByEmail(emails);

        Project project = projects.findByKey(projectKey);
        if (project == null) {
            throw new IllegalArgumentException("No project with key " + projectKey);
        }
        if (users.findById(reporterId) == null) {
            throw new IllegalArgumentException("No user with id " + reporterId);
        }
        Set<Long> memberIds = imports.findMemberIds(project.getId());

        // 3. check every row
        CsvValidator validator = new CsvValidator(project.getId(), reporterId, userIds, memberIds);
        for (CsvRow row : rows) {
            validator.check(row);
        }

        // 4. all or nothing
        if (!validator.getErrors().isEmpty()) {
            return new ImportResult(0, validator.getErrors());
        }
        imports.saveTasks(project.getId(), validator.getTasks());
        return new ImportResult(validator.getTasks().size(), List.of());
    }
}
