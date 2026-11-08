import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

// CSV import only has to work on PostgreSQL (the spec says so).
// In memory every method stops with a clear message instead of crashing.
class InMemoryImportRepository implements ImportRepository {

    private static final String MESSAGE = "CSV import works only with STORAGE=postgres";

    @Override
    public Map<String, Long> findUserIdsByEmail(Collection<String> emails) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public Set<Long> findMemberIds(long projectId) {
        throw new UnsupportedOperationException(MESSAGE);
    }

    @Override
    public void saveTasks(long projectId, List<Task> tasks) {
        throw new UnsupportedOperationException(MESSAGE);
    }
}
