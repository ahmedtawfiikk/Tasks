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
| 3. Transactions | In progress |
| 4. Reports | Done |
| 5. Scale and indexes | Not started |

The schema, the design decisions and the ER diagram are in [docs/SCHEMA.md](docs/SCHEMA.md).

### Files

| Path | What it is |
|---|---|
| `db/migrations/001_create_users.sql` … `008_create_task_status_history.sql` | One table per file. Run them in number order. |
| `db/seed.sql` | Sample data: 6 users, 4 projects, 42 tasks, tags, comments and status history |
| `db/reports.sql` | The five Part 4 reports, one query each, with a comment on how each works. The app reads its queries from this file. |
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
javac -cp "lib/*" -d out *.java Model/*.java Repository/Interfaces/*.java Repository/InMemory/*.java Repository/Postgres/*.java Service/*.java Sort/*.java Filter/*.java Import/*.java Report/*.java
```
```powershell
$env:STORAGE = "postgres"; java -cp "lib/*;out" Main
```
The app shows a numbered menu: create user, create project, add member, create bug or feature task, assign, change status, tag, comment, list tasks (default, filtered by project, status, assignee and tag, sorted, or both; always paginated), show one task with its tags, comments and status history, move a task to another project, import tasks from a CSV file, and the five reports (14 to 18).

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

### Moving a task to another project

Menu option 12 moves a task to another project, in one transaction:
1. Read the names of the task's tags.
2. Delete the task's links to the old project's tags. `task_tags` points at the task **and** its project, so the task can't change project while these links exist.
3. Change the task's `project_id`. If the assignee is not a member of the target project, the foreign key `tasks_assignee_is_member` rejects it.
4. Link each tag again by name: the target project's tag with the same name (ignoring case), or a new one.

Comments and status history point at the task id only, so they stay with the task. If any step fails, everything is rolled back.

**Proving it is atomic.** `FAIL_MOVE_HALFWAY=true` is a hidden test switch: the move throws an exception after step 3, when the tag links are already deleted and the task already points at the new project. On the seed data, task 3 is in PAY with 3 tags, 2 comments and 2 history rows:

```sql
SELECT t.id, p.key, string_agg(g.name || ' (project ' || g.project_id || ')', ', ' ORDER BY g.name) AS tags,
       (SELECT count(*) FROM comments c WHERE c.task_id = t.id) AS comments,
       (SELECT count(*) FROM task_status_history h WHERE h.task_id = t.id) AS history
FROM tasks t
JOIN projects p ON p.id = t.project_id
LEFT JOIN task_tags tt ON tt.task_id = t.id
LEFT JOIN tags g ON g.id = tt.tag_id
WHERE t.id = 3
GROUP BY t.id, p.key;
```
```
 id | key |                         tags                          | comments | history
----+-----+-------------------------------------------------------+----------+---------
  3 | PAY | api (project 1), backend (project 1), urgent (project 1) |        2 |       2
```

Move it to INV with the switch on:
```powershell
$env:STORAGE = "postgres"; $env:FAIL_MOVE_HALFWAY = "true"; $env:SQL_DEBUG = "true"; java -cp "lib/*;out" Main
```
```
Choose: 12
Task id: 3
Target project key: INV
[SQL] SELECT g.name ...
[SQL] DELETE FROM task_tags WHERE task_id = ?
[SQL] UPDATE tasks SET project_id = ? WHERE id = ?
Error: FAIL_MOVE_HALFWAY is on: the move stopped halfway
```
The same query afterwards gives exactly the same row: task 3 is still in PAY with its 3 PAY tags, and the `tags` table still has 9 rows. The `DELETE` and the `UPDATE` were rolled back.

Moving task 3 to SAL fails too, because its assignee (user 1) is not a member of SAL:
```
Error: ERROR: insert or update on table "tasks" violates foreign key constraint "tasks_assignee_is_member"
```

With the switch off (`$env:FAIL_MOVE_HALFWAY = "false"`), moving task 3 to INV works. It reuses INV's `backend` and `urgent` tags and creates `api` in INV, and the comments and history are still there:
```
 id | key |                         tags                          | comments | history
----+-----+-------------------------------------------------------+----------+---------
  3 | INV | api (project 2), backend (project 2), urgent (project 2) |        2 |       2
```

### Importing tasks from CSV

Menu option 13 is `import tasks.csv PAY`: it asks for the file, the project key and the reporter's user id. The CSV has no reporter column, and every task needs a reporter. The columns are:
```
type,title,description,severity,estimate_hours,assignee_email,due_date,tags
```
`tags` is a `|`-separated list. The first line is the header. A value can't contain a comma.

**All or nothing.** Every row is checked before anything is written. If any row is bad, nothing is inserted, and the app prints every bad row with its line number and the reason. The checks are:
- wrong type (not `bug` or `feature`)
- a bug without a severity, or a severity that doesn't exist
- an estimate that is missing, not a number, or out of range (more than 0, at most 200)
- an unknown assignee, or an assignee who is not a member of the project
- a bad due date (not `YYYY-MM-DD`)
- a duplicate title inside the file (trimmed, ignoring case)
- an empty title, a bug with an estimate, a feature with a severity, or a line without 8 columns

Tags repeated in one row (`api|API|api`) count once.

**A fixed number of queries.** However many rows the file has, the import runs the same statements:

| Step | Statement |
|---|---|
| Users of all the emails in the file | `SELECT id, email FROM users WHERE lower(email) = ANY (?)` |
| The project and the reporter | `findByKey`, `findById` |
| The project's members | `SELECT user_id FROM project_members WHERE project_id = ?` |
| Tags, tasks, task tags | 3 `INSERT`s, each sent as one JDBC batch (`addBatch` / `executeBatch`) |

The checks use the looked-up maps, so a row never runs a query. The 3 batches run in one transaction, so killing the app in the middle of an import leaves the database unchanged.

- **Tags:** each tag name of the file is sent once, with `ON CONFLICT DO NOTHING`. The unique index on `(project_id, lower(name))` makes a tag the project already has a conflict, so the database skips it.
- **Tasks:** `getGeneratedKeys()` gives back the new task ids, in the same order as the batch.
- **Task tags:** `INSERT ... SELECT` sends the tag name, and the database finds that tag's id in the project.

The code is split by job, in `Import/`:

| Class | Its one job |
|---|---|
| `CsvReader` | Reads the file and splits each line into a `CsvRow` |
| `CsvValidator` | Checks every row and turns the good ones into tasks with `TaskFactory` |
| `CsvImporter` | Runs the steps in order: read, look up, check, then save all or nothing |
| `ImportResult` | How many tasks were saved, or every bad row |
| `ImportRepository` | The import's database work (`PostgresImportRepository`). In memory mode it prints "CSV import works only with STORAGE=postgres" |

**Sample files** in `samples/`:
- `tasks_valid.csv`: 5 good rows, including `api|API|api` and an email in capitals.
- `tasks_invalid.csv`: 8 different kinds of errors, plus one good row that is not imported either:
```
Nothing was imported. 8 bad rows:
  Line 2: wrong type 'task' (use bug or feature)
  Line 3: a bug needs a severity
  Line 4: estimate 500 is out of range (more than 0, at most 200)
  Line 5: unknown assignee nobody@example.com
  Line 6: assignee ahmed@example.com is not a member of the project
  Line 7: bad due date '2026-13-45' (use YYYY-MM-DD)
  Line 9: duplicate title 'duplicate TITLE' (also on line 8)
  Line 10: has 4 columns, expected 8
```

**Timing: 10,000 rows.** `TasksCsvGenerator` writes a valid file whose assignees are the PAY members from the seed:
```powershell
java -cp out TasksCsvGenerator samples/tasks_10k.csv 10000
```
Importing it into PAY on my machine:
```
Imported 10000 tasks in 2106 ms
```
A second run took 2047 ms. Both are under the 10-second limit. With one bad row added at the end (line 10002), nothing was imported and the task count didn't change.

### Reports

Each report is one SQL query in [db/reports.sql](db/reports.sql), with a comment above it on how it works. There are no `WITH` (CTEs), no window functions and no views. The app runs these same queries, one console command each (14 to 18). The SQL is written only in this file: at startup `SqlFile` reads it, where each `-- name:` line starts a query and the query ends at its `;`, and `PostgresReportRepository` runs each query by its name. Because the path `db/reports.sql` is relative, run the app from the project folder (the `lib/*;out` class path needs that too). The reports work only with `STORAGE=postgres`; in memory they print "Reports work only with STORAGE=postgres".

Reports 1 to 4 also run in psql:
```powershell
psql -d taskmanager -f db/reports.sql
```
Report 5 has 3 `?` parameters (the project, the tag list and its size). psql can't fill them, so it stops with an error on that last query. Run report 5 from the app, with command 18.

The seed has a row for the hard part of each report:

| Report | Hard part | Seed rows |
|---|---|---|
| 1. Project progress | A project with no tasks | Project `WEB` |
| 2. Overdue tasks | An overdue task with no assignee | PAY: `Fix missing refund notification` |
| 3. Top 3 per project | A tie for 3rd place | PAY: Alice Johnson and Mohamed Tawfik, 2 done tasks each |
| 4. Members with no tasks | A member with zero tasks, in a project that has an unassigned task | Ziad Mohamed in PAY |
| 5. Tasks with all given tags | A task with only some of the tags | PAY: `Fix card declined error` (only `backend`) and `Fix receipt email typo` (only `urgent`, plus `frontend`) |

The output below is from the app on the seed data.

**1. Project progress** (command 14). `WEB` has no tasks and shows 0 everywhere and 0.0%, not NULL and with no division error. `LEFT JOIN` keeps it, `COUNT(t.id)` counts it as 0, and `GREATEST(total, 1)` divides it by 1.
```
Key   Project       Total   Done  In progress  Pending   % done
INV   Inventory        14      8            2        4     57.1
PAY   Payments         18     13            2        3     72.2
SAL   Sales            10      6            2        2     60.0
WEB   Website           0      0            0        0      0.0
```

**2. Overdue tasks** (command 15, run on 2026-11-08; the days late depend on today's date). The PAY task with no assignee shows `Unassigned`. `LEFT JOIN users` keeps it, and `COALESCE` gives the word.
```
Key   Task                                Assignee         Days late
SAL   Fix tax rate per region             Ziad Mohamed            20
PAY   Fix missing refund notification     Unassigned              18
INV   Add purchase orders                 Ahmed Tawfik            16
PAY   Fix slow transaction search         Jane Smith              15
INV   Fix slow product search             Jane Smith              12
SAL   Add CRM sync                        Ahmed Tawfik             8
INV   Fix negative stock after refund     Ahmed Tawfik             6
INV   Add inventory dashboard             John Doe                 4
PAY   Add fraud score check               Mohamed Tawfik           1
```

**3. Top 3 people per project** (command 16). In PAY, Alice and Mohamed tie for 3rd with 2 done tasks each, so both show. The query finds the 3rd-place score of the project (`ORDER BY ... LIMIT 1 OFFSET 2`) and keeps everyone with at least that score. In INV, Mohamed has 1 done task, below the 3rd-place score of 2, so he is left out.
```
Key   Person           Done tasks
INV   Jane Smith                3
INV   Ahmed Tawfik              2
INV   John Doe                  2
PAY   John Doe                  5
PAY   Jane Smith                4
PAY   Alice Johnson             2
PAY   Mohamed Tawfik            2
SAL   Ahmed Tawfik              3
SAL   Alice Johnson             2
SAL   Ziad Mohamed              1
```

**4. Members with no tasks** (command 17 prints all three versions). `NOT EXISTS` and `LEFT JOIN ... IS NULL` give the same result. Ziad has tasks in SAL but none in PAY, so he shows for PAY only. `NOT IN` gives the wrong answer: it misses Ziad.
```
NOT EXISTS:
  PAY   Ziad Mohamed
  WEB   Ahmed Tawfik
  WEB   Mohamed Tawfik
LEFT JOIN ... IS NULL:
  PAY   Ziad Mohamed
  WEB   Ahmed Tawfik
  WEB   Mohamed Tawfik
NOT IN (wrong: a task with no assignee puts a NULL in the list):
  WEB   Ahmed Tawfik
  WEB   Mohamed Tawfik
```
Why `NOT IN` is wrong: for PAY, the subquery lists the assignee ids of PAY's tasks, and `Fix missing refund notification` has no assignee, so the list holds a NULL. `Ziad NOT IN (1, 2, ..., NULL)` means `Ziad <> 1 AND Ziad <> 2 AND ... AND Ziad <> NULL`. A comparison with NULL is unknown, never true, so the whole condition is never true and every PAY member is dropped. WEB has no tasks, so its list is empty and has no NULL, which is why WEB still shows. `NOT EXISTS` and `LEFT JOIN` never compare with the NULL, so they get it right.

**5. Tasks with all given tags** (command 18). The app passes the tag names as one array parameter and the number of names, so the list can have any length. For each task, `COUNT(*)` counts how many of the listed tags it has, and `HAVING COUNT(*) = list size` keeps only the tasks that have all of them. The names are made lower case and repeats are removed first, so `Backend, URGENT, backend` counts as 2 tags.

PAY with `backend,urgent`. `Fix card declined error` (#1, only `backend`) and `Fix receipt email typo` (#11, only `urgent`) have only some of the tags, so they are not listed:
```
#3  Fix double charge on retry
#15  Fix slow transaction search
#18  Fix missing refund notification
```
More cases, all checked:

| Project and tags | Result |
|---|---|
| PAY `Backend, URGENT, backend` | Same 3 tasks (case and repeats ignored) |
| PAY `api,backend,urgent` (3 tags) | #3 only |
| PAY `backend` (1 tag) | #1, #2, #3, #6, #15, #18 |
| PAY `backend,nosuchtag` | No task has all of these tags. |
| INV `backend,urgent` | #19 only (each project has its own tags) |
| PAY with no tag | Error: Give at least one tag |
