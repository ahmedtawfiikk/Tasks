import java.util.List;

// The five reports of Part 4. Each method runs one SQL query (the same ones as db/reports.sql).
// The reports are SQL questions, so they work only on PostgreSQL; in memory every method throws a clear message.
interface ReportRepository {

    // 1. every project, also one with no tasks
    List<ProjectProgress> projectProgress();

    // 2. tasks not done whose due date has passed, most late first
    List<OverdueTask> overdueTasks();

    // 3. the 3 people who finished the most tasks in each project (a tie for 3rd shows both)
    List<TopPerson> topPeople();

    // 4. members with no task in their project, written three ways
    List<MemberWithoutTasks> membersWithoutTasksNotExists();

    List<MemberWithoutTasks> membersWithoutTasksLeftJoin();

    // wrong on purpose: misses members of a project that has a task with no assignee
    List<MemberWithoutTasks> membersWithoutTasksNotIn();

    // 5. tasks of the project that have every one of these tags (any number of tags)
    List<TaskTitle> tasksWithAllTags(long projectId, List<String> tagNames);
}
