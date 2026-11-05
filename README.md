# Task Manager

## Assignments 1 and 2: the in-memory app

1. splited into two main classes task and task manager and we have featueres each one do

TaskManger :

- Add a task
- Find a task by id instantly (no looping through all tasks)
- List all tasks
- Mark a task as done


Tasks:
- id, a title, a status (pending or done) and tags
- Task types   BugTask (has a severity: low, medium, high)  ,   FeatureTask (has an estimate in hours).


2. Sorting
- TaskManager can list tasks sorted in different ways: by title, by status (pending first), or in the order they were added.
- Each way of sorting is its own class, and they all share the same interface.
- Adding a new sort order later should only mean adding one new class, with no changes to TaskManager.

3. Design patterns:
- we use (factory method) in Task diffrent types creation
- we use (Strategy pattern) in sorting with diffrent ways 
- we use (Strategy pattern) again in filtering with diffrent rules

4. Filtering
- TaskManager can list only the tasks that match a filter: by status, by tag, or by bug severity.
- Filters can be combined with AndFilter (for example: tagged "urgent" AND still pending).
- A filter and a sort can be used together: listTasks(filter, sortStrategy).
- Each filter is its own class sharing the TaskFilter interface, so adding a new filter later only means adding one new class, with no changes to TaskManager.
- In Assignment 3, the in-memory repository still lists tasks with these filters, plus `FilterByProject` and `FilterByAssignee`. PostgreSQL filters in the `WHERE` of its query instead, so it never loads tasks that don't match.

and aslo we save changes by github and git version control

## Assignment 3: PostgreSQL database

The task manager becomes a multi-user, multi-project tracker stored in PostgreSQL.

| Part | Status |
|---|---|
| 1. Schema, migrations, seed data | Done |
| 2. Data layer (JDBC) | Done |
| 3. Transactions | Not started |
| 4. Reports | Not started |
| 5. Scale and indexes | Not started |

The schema, the design decisions and the ER diagram are in [docs/SCHEMA.md](docs/SCHEMA.md).

### Files

| Path | What it is |
|---|---|
| `db/migrations/001_create_users.sql` … `008_create_task_status_history.sql` | One table per file. Run them in number order. |
| `db/seed.sql` | Sample data: 6 users, 4 projects, 42 tasks, tags, comments and status history |
| `docs/SCHEMA.md` | ER diagram, how each rule is enforced, and the design decisions |

### Set up the database from zero

**1. Install** PostgreSQL 17 and make sure `psql` and `createdb` are on your `PATH`.

**2. Set the connection settings as environment variables.** `psql` reads these standard names, and the app will read the same ones. Never put the password in a file in this repo.

```powershell
[Environment]::SetEnvironmentVariable("PGHOST", "localhost", "User")
[Environment]::SetEnvironmentVariable("PGPORT", "5432", "User")
[Environment]::SetEnvironmentVariable("PGDATABASE", "taskmanager", "User")
[Environment]::SetEnvironmentVariable("PGUSER", "postgres", "User")
[Environment]::SetEnvironmentVariable("PGPASSWORD", "<your password>", "User")
```
Open a new terminal afterwards so it picks up the variables.

**3. Create the database:**
```powershell
createdb taskmanager
```

**4. Run the migrations in order.** If a file has an error, the loop stops there, and nothing from that file is saved (that's what `-1` does).
```powershell
Get-ChildItem db\migrations\*.sql | Sort-Object Name | ForEach-Object {
  Write-Host "== $($_.Name)"
  psql -v ON_ERROR_STOP=1 -1 -f $_.FullName
  if ($LASTEXITCODE -ne 0) { throw "Migration failed: $($_.Name)" }
}
```

**5. Load the seed data.** It empties all tables first, so you can run it again at any time.
```powershell
psql -v ON_ERROR_STOP=1 -f db/seed.sql
```

**6. Check the result:**
```powershell
psql -c "SELECT p.key, count(t.id) AS tasks FROM projects p LEFT JOIN tasks t ON t.project_id = p.id GROUP BY p.key ORDER BY p.key;"
```
Expected: INV 14, PAY 18, SAL 10, WEB 0.

**To start over**, delete everything and repeat steps 4 and 5:
```powershell
psql -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

### The database enforces its own rules

Bad data is rejected by PostgreSQL itself, even when typed straight into `psql`. For example, a bug must have a severity:
```sql
INSERT INTO tasks (project_id, title, reporter_id, type) VALUES (1, 'Broken login', 1, 'bug');
-- ERROR: new row for relation "tasks" violates check constraint "tasks_type_fields"
```
And a task can't be assigned to someone outside its project. Ahmed (user 4) is not a member of PAY (project 1):
```sql
INSERT INTO tasks (project_id, title, reporter_id, assignee_id, type, severity) VALUES (1, 'Broken login', 1, 4, 'bug', 'high');
-- ERROR: insert or update on table "tasks" violates foreign key constraint "tasks_assignee_is_member"
```
Every rule and how it is enforced is listed in [docs/SCHEMA.md](docs/SCHEMA.md#how-the-database-enforces-the-rules).

### Run the app

Compile, then run. `lib/` holds the PostgreSQL JDBC driver, the only dependency.
```powershell
javac -cp "lib/*" -d out *.java Model/*.java Repository/Interfaces/*.java Repository/InMemory/*.java Repository/Postgres/*.java Service/*.java Sort/*.java Filter/*.java
```
```powershell
$env:STORAGE = "postgres"; java -cp "lib/*;out" Main
```
The app shows a numbered menu: create user, create project, add member, create bug or feature task, assign, change status, tag, comment, list tasks (default, filtered by project, status, assignee and tag, sorted, or both; always paginated) and show one task with its tags, comments and status history.

To print every SQL statement the app runs, turn on the debug flag:
```powershell
$env:SQL_DEBUG = "true"
```

### Choosing the storage

One environment variable, `STORAGE`, picks where the data lives. Nothing else in the code changes.

| `STORAGE` | Storage |
|---|---|
| `memory` (or not set) | In-memory maps, as in Assignments 1 and 2 |
| `postgres` | PostgreSQL, using the `PG*` connection settings above |

**Design pattern: Abstract Factory.** `RepositoryFactory` is an interface with one method per repository: `createUserRepository()`, `createProjectRepository()` and `createTaskRepository()`. Each storage has its own factory class: `InMemoryRepositoryFactory` and `PostgresRepositoryFactory`. `Main` reads `STORAGE` once, picks the factory, and gives the repositories it creates to `TaskManager`.

Why this pattern:
- The three repositories are a **family** that must work together. For example, the task repository reads assignee names from the user repository. One factory creates the whole family, so memory and PostgreSQL repositories can never be mixed.
- `TaskManager` only sees the repository interfaces. It never knows which factory made them.
- Adding a storage means adding one new factory class and one new `STORAGE` value.

### The data layer

The repository code is split by storage:
- `Repository/Interfaces/` holds the repository interfaces and `RepositoryFactory`, which both storages share.
- `Repository/InMemory/` holds the in-memory set.
- `Repository/Postgres/` holds the PostgreSQL set, with `Database`, `SqlLogger` and `RowMapper`.

| Class | Its one job |
|---|---|
| `TaskManager` | Business operations. Knows only the repository interfaces, never imports `java.sql` |
| `UserRepository`, `ProjectRepository`, `TaskRepository` | The interfaces: what can be saved and loaded |
| `InMemory…Repository` | Stores data in `HashMap`s. Finding a task by id is O(1). The task list is filtered with the `TaskFilter` classes. Tags are kept as `Tag` objects per project, like the `tags` table, so tagging behaves the same as in PostgreSQL. The repositories also check the database's rules by hand (unique email and key, key format, estimate range, no empty text, users exist, an assignee is a project member), so bad input fails the same way in both storages |
| `InMemoryChecks` | The checks the in-memory repositories share: "not empty" and "user exists" |
| `Postgres…Repository` | Builds and runs the SQL, always with `PreparedStatement` |
| `Database` | Reads the `PG*` settings, opens connections, commits or rolls back transactions |
| `SqlLogger` | Prints every statement when `SQL_DEBUG=true` |
| `RowMapper` | Turns rows into objects. A task row goes through `TaskFactory`, which makes a `BugTask` or a `FeatureTask` |
| `ConsoleApp` | Reads commands and prints results |

Every `Connection`, `PreparedStatement` and `ResultSet` is closed with try-with-resources. Creating a project with its owner, tagging a task and changing a status each run in one transaction. A task is created without tags, with one `INSERT`; tags are added after, with "tag".

### Sorting in memory and in SQL

Each sort strategy works in both storages:
```java
interface SortStrategy {
    Comparator<Task> comparator();   // used by the in-memory repository
    SortColumn column();             // used by the PostgreSQL repository in ORDER BY
    default boolean descending() { return false; }
}
```
PostgreSQL sorts the rows itself, with `LIMIT` and `OFFSET`. Java never loads all tasks to sort them.

**Adding a sort** is one new class, for example `SortByDueDate` returning `SortColumn.DUE_DATE`. The console picks a sort by its class name, so `TaskManager`, the repositories and the console don't change.

**Why `ORDER BY` can't be injected.** A `PreparedStatement` parameter can only be a value, never a column name, so `ORDER BY ?` doesn't work. Instead, a strategy returns a `SortColumn`, an enum of fixed column names such as `t.title`. The repository puts only that fixed text into `ORDER BY`. A strategy can't return free text, and nothing typed by the user ever reaches the SQL. Every filter value, the page size and the offset are still `?` parameters.

**Same order on every page.** `ORDER BY` always ends with `t.id`. Two tasks with the same title are always in id order, so they never swap between pages. The in-memory repository does the same: it sorts by id first, and Java's sort keeps equal items in that order.

### Listing tasks without N+1

"List tasks in project X with their tags and assignee name, page 3, 20 per page" runs **2 queries**, however many tasks are on the page:
1. The page of tasks, with `LEFT JOIN users` for the assignee's name, `ORDER BY …, t.id` and `LIMIT ? OFFSET ?`. The `WHERE` is built in Java from only the filters the user chose: it starts as `WHERE 1 = 1`, and each chosen filter adds a fixed text such as `AND t.project_id = ?`. The values still go in `?` parameters.
2. The tags of all those tasks at once: `WHERE tt.task_id = ANY (?)`, with the task ids as one array parameter.

The tasks from query 1 go into a `HashMap` by id. Query 2's rows are read once, and each row finds its task in the map in O(1). That's one pass, with no loop inside a loop. Turn on `SQL_DEBUG=true` and run "List tasks" to see the 2 statements.
