-- Part 5: the measured queries (the task list, report 2, report 3 and report 5),
-- run with EXPLAIN (ANALYZE, BUFFERS) on taskmanager_big.
-- before.txt and after.txt hold their plans in this same order: task list, report 2, report 3, report 5.
-- \qecho writes a line into the output file (-o), so each plan there starts with the name of its query.
-- Each one is the same SQL the app runs; psql can't fill ? marks, so they are replaced by values:
-- project 1 is PAAA, the biggest project (about 111,000 tasks).
--   psql -d taskmanager_big -f docs/explain/queries.sql -o docs/explain/before.txt


-- Task list: the paginated task list from Part 2 (PostgresTaskRepository.findTasks), console command 9
-- project 1, sorted by title, page 3, 20 per page -> LIMIT 20 OFFSET 40
\qecho '=================================================================='
\qecho '== Task list: the paginated task list from Part 2 (console command 9), project 1, by title, page 3'
\qecho '=================================================================='
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.id, t.project_id, t.type, t.title, t.description, t.status, t.reporter_id,
       t.assignee_id, t.due_date, t.severity, t.estimate_hours, t.created_at, t.completed_at,
       u.full_name AS assignee_name
FROM tasks t
LEFT JOIN users u ON u.id = t.assignee_id
WHERE 1 = 1 AND t.project_id = 1
ORDER BY t.title, t.id
LIMIT 20 OFFSET 40;


-- Report 2: overdue tasks (overdue_tasks in db/reports.sql), console command 15
\qecho '=================================================================='
\qecho '== Report 2: overdue tasks (console command 15)'
\qecho '=================================================================='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.key, t.title,
       COALESCE(u.full_name, 'Unassigned') AS assignee,
       CURRENT_DATE - t.due_date           AS days_late
FROM tasks t
JOIN projects p ON p.id = t.project_id
LEFT JOIN users u ON u.id = t.assignee_id
WHERE t.status <> 'done' AND t.due_date < CURRENT_DATE
ORDER BY t.due_date, t.id;


-- Report 3: top 3 people per project (top_people in db/reports.sql), console command 16
\qecho '=================================================================='
\qecho '== Report 3: top 3 people per project (console command 16)'
\qecho '=================================================================='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.key,
       u.full_name,
       COUNT(t.id) AS done_tasks
FROM tasks t
JOIN projects p ON p.id = t.project_id
JOIN users u ON u.id = t.assignee_id
WHERE t.status = 'done'
GROUP BY p.id, u.id
HAVING COUNT(t.id) >= COALESCE((SELECT COUNT(*)
                                FROM tasks d
                                WHERE d.project_id = p.id AND d.status = 'done' AND d.assignee_id IS NOT NULL
                                GROUP BY d.assignee_id
                                ORDER BY COUNT(*) DESC
                                LIMIT 1 OFFSET 2), 0)
ORDER BY p.key, done_tasks DESC, u.full_name;


-- Report 5: tasks with all given tags (tasks_with_all_tags in db/reports.sql), console command 18
-- the 3 ? marks: project 1, the tags backend and urgent, and the list size 2
\qecho '=================================================================='
\qecho '== Report 5: tasks with all given tags (console command 18), project 1, backend and urgent'
\qecho '=================================================================='
EXPLAIN (ANALYZE, BUFFERS)
SELECT t.id,
       t.title
FROM tasks t
JOIN task_tags tt ON tt.task_id = t.id
JOIN tags g ON g.id = tt.tag_id
WHERE t.project_id = 1 AND lower(g.name) = ANY (ARRAY['backend', 'urgent'])
GROUP BY t.id, t.title
HAVING COUNT(*) = 2
ORDER BY t.id;
