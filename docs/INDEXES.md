# Indexes (Part 5)

## How it was measured

- **Data:** `taskmanager_big`, filled by `Scale/DataGenerator.java` with 2,000 users, 50 projects, 500,000 tasks, 1,248,549 task tags and 774,990 history rows (see the README).
- **Queries:** the task list, report 2, report 3 and report 5, in [explain/queries.sql](explain/queries.sql). The task list is the paginated list from Part 2 (console command 9). The reports are the Part 4 reports with the same numbers (commands 15, 16 and 18). Each is the same SQL the app runs, with the `?` marks replaced by values. Project 1 (`PAAA`) is the biggest project, with 111,406 tasks.
- **Before:** only the primary keys and unique constraints from migrations 001 to 008.
- **After:** migration [009_add_indexes.sql](../db/migrations/009_add_indexes.sql) adds 4 indexes.
- Before each measurement: `VACUUM ANALYZE` (fresh statistics), then the file is run twice and the second run is kept, so the tables are already in memory both times.
- The full `EXPLAIN (ANALYZE, BUFFERS)` output is in [explain/before.txt](explain/before.txt) and [explain/after.txt](explain/after.txt), in this order: task list, report 2, report 3, report 5.

```powershell
psql -d taskmanager_big -f docs/explain/queries.sql -o docs/explain/before.txt
psql -d taskmanager_big -v ON_ERROR_STOP=1 -1 -f db/migrations/009_add_indexes.sql
psql -d taskmanager_big -f docs/explain/queries.sql -o docs/explain/after.txt
```

## Results

| Query | Plan before | Time before | Plan after | Time after |
|---|---|---|---|---|
| Task list: project 1, by title, page 3 | Seq Scan on tasks, then a top-N sort of 111,406 rows | 155 ms | Index Scan on `tasks_project_title_id`, stops after 60 rows | 0.3 ms |
| Report 2: overdue tasks | Seq Scan on tasks, Hash Joins, a sort of 97,976 rows that spills to disk | 180 ms | Index Only Scan on `tasks_overdue`, already in order: no sort | 90 ms |
| Report 3: top 3 per project | Seq Scan on tasks, and the subquery does another Seq Scan of all tasks 990 times | 54,839 ms | Index Scan, and the subquery is an Index Only Scan on `tasks_done_project_assignee` | 1,197 ms |
| Report 5: tasks with all tags | Seq Scan on task_tags (1,248,549 rows) and on tasks | 167 ms | Index Only Scan on `task_tags_tag_task` and on `tasks_project_title_id` | 90 ms |

The times move a little from run to run. In an earlier run of the same "before" measurement, the task list took 116 ms and report 3 took 67 s.

## The indexes

### 1. `tasks_project_title_id ON tasks (project_id, title, id)`

**Serves:** the paginated task list: `WHERE t.project_id = ? ORDER BY t.title, t.id LIMIT ? OFFSET ?`.

**Why this column order:**
1. `project_id` comes first because the query asks for one project with `=`. All of a project's entries sit together in the index.
2. `title` comes next because the query sorts by title. Inside one project, the index entries are already in title order.
3. `id` comes last because `ORDER BY` always ends with `t.id`, so that tasks with the same title keep their order between pages. With `id` in the index, ties are already in the right order too.

Because the index order is exactly the `ORDER BY`, the database doesn't sort at all. It walks the index from the start of project 1 and stops after `OFFSET + LIMIT` = 60 entries.

In the other order, `(title, project_id)`, the entries of one project would be spread over the whole index. The database would have to walk the titles of every project and throw most of them away.

**Plan:** from `Parallel Seq Scan on tasks` (about 130,000 rows removed by the filter, per worker) plus `top-N heapsort`, to `Index Scan using tasks_project_title_id` (rows=60). **155 ms → 0.3 ms.**

**Limit:** a very deep page (for example `OFFSET 100000`) still walks 100,000 index entries before it reaches the page. `OFFSET` always reads the rows it skips.

### 2. `tasks_overdue ON tasks (due_date, id) INCLUDE (project_id, assignee_id, title) WHERE status <> 'done'`

**Serves:** report 2, overdue tasks.

**Why it was slow:** the report returns 97,976 rows (20% of all tasks). Before, the database read the whole table, joined those rows with projects and users, then sorted them. The sort didn't fit in memory and spilled to disk.

**What each part of the index does:**
1. **`WHERE status <> 'done'`** (a partial index) keeps only tasks that are not done, about 30% of the table. Because the query's `WHERE` contains the same `status <> 'done'`, the planner knows it can use this index.
2. **`(due_date, id)`** is the order of the report, so the rows come out of the index already sorted, and the database doesn't sort. `due_date` first also makes `due_date < CURRENT_DATE` one range at the start of the index. `id` second matches the tie-breaker of the `ORDER BY`.
3. **`INCLUDE (project_id, assignee_id, title)`** copies the other columns the report reads into the index, without making them part of the search key. With everything in the index, the scan is an Index Only Scan and never reads the table (`Heap Fetches: 0`).

**The query had to change too.** The report used to say `ORDER BY days_late DESC, t.id`, where `days_late` is `CURRENT_DATE - t.due_date`. The planner can't tell that sorting by that expression is the same as sorting by `due_date`, so it sorted all 97,976 rows even with the index. Now `db/reports.sql` says `ORDER BY t.due_date, t.id`:
- The earliest due date is the most days late, so it's the same order.
- Tasks with the same due date have the same days late, and `t.id` breaks the tie in both versions.

I checked this on all 97,976 overdue rows: both `ORDER BY`s give exactly the same order.

**Plan:** from `Parallel Seq Scan on tasks` plus `Sort Method: external merge Disk`, to `Index Only Scan using tasks_overdue` with no Sort node. **180 ms → 90 ms.**

The report still returns about 98,000 rows, which it has to join with projects and users. That's why it doesn't get as fast as the task list.

### 3. `tasks_done_project_assignee ON tasks (project_id, assignee_id) WHERE status = 'done'`

**Serves:** report 3, top 3 people per project. This was the slowest query by far.

**Why it was so slow:** the `HAVING` subquery finds the 3rd-place score of one project:
```sql
SELECT COUNT(*) FROM tasks d
WHERE d.project_id = p.id AND d.status = 'done' AND d.assignee_id IS NOT NULL
GROUP BY d.assignee_id ORDER BY COUNT(*) DESC LIMIT 1 OFFSET 2
```
It runs once for every (project, person) group of the outer query, which is **990 times**. Without an index, each run was a `Seq Scan on tasks d` that read all 500,000 rows and removed 493,846 of them. That is about 495 million rows read in total.

**Why this column order:**
1. `project_id` comes first because the subquery asks for one project with `=` (`d.project_id = p.id`).
2. `assignee_id` comes second because the subquery groups by it (`GROUP BY d.assignee_id`) and checks `IS NOT NULL`. Inside one project, the entries are already grouped by assignee.

**Why `WHERE status = 'done'` (a partial index):**
- Only done tasks matter to this report, so the 30% of tasks that are not done are left out of the index.
- Because the query's `WHERE` contains `status = 'done'`, the planner knows it can use this index.

**Why it is only 2.5 MB:** many tasks share the same `(project_id, assignee_id)`. Since PostgreSQL 13, a B-tree stores repeated keys once (deduplication).

**Plan:** the subquery went from `Seq Scan on tasks d` (loops=990) to `Index Only Scan using tasks_done_project_assignee` (loops=990, about 6,154 index entries each, never touching the table). The outer query also uses the index (`Index Scan`) instead of a Seq Scan and an external sort on disk. **54,839 ms → 1,197 ms.**

It is still about 1 second because the subquery still runs 990 times; each run is just cheap now. Running it only once per project would need a window function or a `WITH`, which the assignment forbids.

### 4. `task_tags_tag_task ON task_tags (tag_id, task_id)`

**Serves:** report 5, tasks with all given tags.

**Why it's needed:** the primary key of `task_tags` is `(task_id, tag_id)`. It answers "what are the tags of this task", which is what the task list's second query asks. Report 5 asks the opposite question: "which tasks have this tag". An index can only be searched from its first column, so the primary key can't help. Before, the database read all 1,248,549 rows of `task_tags`.

**Why this column order:**
1. `tag_id` comes first because the query starts from the tags in the list and looks up their task links (`tt.tag_id = g.id`).
2. `task_id` comes second because it is the other column the query needs. With both columns in the index, the lookup is an Index Only Scan and never reads the table.

**Plan:** from `Parallel Seq Scan on task_tags` to `Index Only Scan using task_tags_tag_task` (100 lookups, about 1,249 rows each). The tasks of project 1 now come from an `Index Only Scan using tasks_project_title_id`, because index 1 holds `project_id`, `title` and `id`, which is everything this query needs from `tasks`. **167 ms → 90 ms.**

The 100 lookups happen because the tags table is searched by name in every project: 2 names × 50 projects. Only the tags of project 1 lead to tasks of project 1.

## Indexes that did not help

### `tasks (status)`: the planner ignored it

Tried for report 2, in a transaction that was rolled back. The plan stayed `Parallel Seq Scan on tasks`, at the same time as before. Two reasons:
1. **A B-tree can't search for `<>`.** It can find `=`, `<`, `>` and ranges, but "everything except done" is not one range in the index. The planner has no way to use it for `status <> 'done'`.
2. **Even `status = 'done'` matches 70% of the table.** With only 3 values, any status matches a big part of the table. Reading most of the table through an index is slower than reading the table straight through, so the planner prefers the Seq Scan.

### `tasks (due_date) WHERE status <> 'done'`: used, but not faster

My first try for report 2. The planner did use it (`Bitmap Index Scan` instead of `Seq Scan`), but the time didn't really change: about 140 ms before, and 111 to 152 ms with it, depending on the run.
- Finding the 97,976 overdue rows was never the slow part. The database still had to read each row from the table to get its title and assignee.
- It still had to sort all of them, because the `ORDER BY` was on `days_late`.

Index 2 above fixes both: `INCLUDE` removes the table reads, and the rewritten `ORDER BY` removes the sort.

Two other tries for report 2, also not kept:
- **Only rewriting the `ORDER BY`**, with no index, changed nothing (about 141 ms). The rows still came from a Seq Scan in no order.
- **Giving the sort more memory** (`work_mem = 64MB`) let the sort finish in memory but wasn't faster (159 ms). The sort was not the only cost.

## What the indexes cost on writes

Every index is a second copy of some columns that the database must keep up to date. Each `INSERT` adds an entry to every index of the table, each `DELETE` removes one, and an `UPDATE` of an indexed column changes the entry.

| Index | Size on 500k tasks | Written by |
|---|---|---|
| `tasks_project_title_id` | 28 MB | every new task; changing a task's title; moving a task to another project |
| `tasks_overdue` | 11 MB | every new task (new tasks are pending, so they are "not done"), which includes every row of the CSV import; changing the title, assignee or due date of a task that is not done; a status change to done removes the entry, and a reopen adds it back |
| `tasks_done_project_assignee` | 2.5 MB | only tasks whose status is `done`: a status change to or from done, and assigning a done task. New tasks are pending, so creating a task and the CSV import don't write it |
| `task_tags_tag_task` | 38 MB | every tag link added (tag, CSV import) or removed (moving a task) |

For comparison, the `tasks` table is 61 MB and `task_tags` is 62 MB.

Creating the 4 indexes on the 500k-task database took about 3.6 s: `tasks_project_title_id` 2.0 s, `task_tags_tag_task` 1.1 s, and `tasks_overdue` and `tasks_done_project_assignee` 0.3 s each.

**CSV import with all indexes in place.** The same `samples/tasks_10k.csv` was imported into PAY 3 times, in a fresh database (migrations, then `db/seed.sql`), once without and once with migration 009:

| | Run 1 | Run 2 | Run 3 | Average |
|---|---|---|---|---|
| Migrations 001–008 | 2,043 ms | 2,050 ms | 2,106 ms | 2,066 ms |
| With 009 (the 4 new indexes) | 2,382 ms | 2,530 ms | 2,392 ms | 2,435 ms |

The import got about **18% slower**:
- Each of its 10,000 tasks writes two more index entries (`tasks_project_title_id` and `tasks_overdue`).
- Each of its task tags writes one more (`task_tags_tag_task`).

It is still far under the 10-second limit. In exchange, report 3 is about 45 times faster, the task list about 500 times faster, and reports 2 and 5 about twice as fast. That trade is worth it for an app that reads much more often than it writes.
