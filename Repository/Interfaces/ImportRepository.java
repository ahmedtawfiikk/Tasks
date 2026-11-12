import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

// What the CSV import (Part 3.3) needs from the storage.
// Each method uses a fixed number of queries for the whole file, never one query per row.
// The spec only asks for CSV import on PostgreSQL; in memory every method throws a clear message.
interface ImportRepository {

    // the ids of the users with these emails (given in lower case): lower-case email -> user id
    Map<String, Long> findUserIdsByEmail(Collection<String> emails);

    // the ids of every member of the project, the owner included
    Set<Long> findMemberIds(long projectId);

    // saves all the tasks with their tags in one transaction: all of them or none
    void saveTasks(long projectId, List<Task> tasks);
}
