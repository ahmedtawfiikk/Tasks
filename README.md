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

and aslo we save changes by github and git version control

## Assignment 3: PostgreSQL database

The task manager becomes a multi-user, multi-project tracker stored in PostgreSQL.

| Part | Status |
|---|---|
| 1. Schema, migrations, seed data | Done |
| 2. Data layer (JDBC) | Not started |
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
