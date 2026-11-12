import java.util.List;

// The reports are SQL queries (Part 4), so they work only on PostgreSQL.
// In memory every method stops with a clear message instead of crashing.
class InMemoryReportRepository implements ReportRepository {

    private static final String MESSAGE = "Reports work only with STORAGE=postgres";

    @Override
    public List<ProjectProgress> projectProgress() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<OverdueTask> overdueTasks() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<TopPerson> topPeople() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<MemberWithoutTasks> membersWithoutTasksNotExists() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<MemberWithoutTasks> membersWithoutTasksLeftJoin() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<MemberWithoutTasks> membersWithoutTasksNotIn() {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public List<TaskTitle> tasksWithAllTags(long projectId, List<String> tagNames) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
