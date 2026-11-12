-- Part 4: the five reports. Each report is one SQL query.
-- The app reads its queries from this file (SqlFile.java), one console command per report.
-- Each "-- name:" line starts a query, and the query ends at its ;
-- No WITH, no window functions, no views.


-- 1. Project progress
-- name: project_progress
-- Start from projects and LEFT JOIN tasks, so a project with no tasks still gets one row
-- (its task columns are NULL). COUNT(t.id) skips NULLs, so that project counts 0, not 1.
-- FILTER makes a COUNT count only the rows with that status.
-- 100.0 keeps the division decimal. GREATEST(total, 1) never divides by zero:
-- a project with no tasks divides 0 by 1 and gets 0.0.
SELECT p.key, p.name,
       COUNT(t.id)                                        AS total,
       COUNT(t.id) FILTER (WHERE t.status = 'done')        AS done,
       COUNT(t.id) FILTER (WHERE t.status = 'in_progress') AS in_progress,
       COUNT(t.id) FILTER (WHERE t.status = 'pending')     AS pending,
       ROUND(100.0 * COUNT(t.id) FILTER (WHERE t.status = 'done') / GREATEST(COUNT(t.id), 1), 1)                AS percent_done
FROM projects p
LEFT JOIN tasks t ON t.project_id = p.id
GROUP BY p.id
ORDER BY p.key;


-- 2. Overdue tasks
-- name: overdue_tasks
-- Tasks that are not done and whose due date is before today.
-- A task with no due date drops out by itself: NULL < CURRENT_DATE is never true.
-- LEFT JOIN users, because the assignee can be NULL and that task must still show.
-- COALESCE turns the missing name into 'Unassigned'.
-- A date minus a date is a number of days. Most late first.
SELECT p.key, t.title,
       COALESCE(u.full_name, 'Unassigned') AS assignee,
       CURRENT_DATE - t.due_date           AS days_late
FROM tasks t
JOIN projects p ON p.id = t.project_id
LEFT JOIN users u ON u.id = t.assignee_id
WHERE t.status <> 'done' AND t.due_date < CURRENT_DATE
ORDER BY days_late DESC, t.id;


-- 3. Top 3 people per project
-- name: top_people
-- The outer query counts the done tasks of each person in each project
-- (JOIN users: a done task with no assignee counts for nobody).
-- The subquery finds the 3rd-place score of the same project: everyone's done count,
-- high to low, skip 2 (OFFSET 2), take 1 (LIMIT 1).
-- HAVING keeps everyone with at least that score, so a tie for 3rd shows both people.
-- With fewer than 3 people there is no 3rd row: the subquery gives NULL, and COALESCE makes it 0.
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


-- 4a. Members with no tasks, with NOT EXISTS
-- name: members_without_tasks_not_exists
-- For each membership, keep it when no task in that project is assigned to that user.
SELECT p.key,
       u.full_name
FROM project_members m
JOIN projects p ON p.id = m.project_id
JOIN users u ON u.id = m.user_id
WHERE NOT EXISTS (SELECT 1
                  FROM tasks t
                  WHERE t.project_id = m.project_id AND t.assignee_id = m.user_id)
ORDER BY p.key, u.full_name;


-- 4b. Members with no tasks, with LEFT JOIN ... IS NULL
-- name: members_without_tasks_left_join
-- LEFT JOIN the member's tasks in that project. A member with no task gets one row
-- whose task columns are NULL, so t.id IS NULL keeps exactly those members.
-- Same result as 4a.
SELECT p.key, u.full_name
FROM project_members m
JOIN projects p ON p.id = m.project_id
JOIN users u ON u.id = m.user_id
LEFT JOIN tasks t ON t.project_id = m.project_id AND t.assignee_id = m.user_id
WHERE t.id IS NULL
ORDER BY p.key, u.full_name;


-- 4c. Members with no tasks, with NOT IN: WRONG on purpose
-- name: members_without_tasks_not_in
-- The subquery lists the assignee ids of the project. PAY has a task with no assignee,
-- so the list holds a NULL. "x NOT IN (2, 3, NULL)" means x <> 2 AND x <> 3 AND x <> NULL,
-- and x <> NULL is NULL (unknown), never true. So every PAY member is dropped,
-- and Ziad (a PAY member with zero tasks) is missing. NOT EXISTS and LEFT JOIN don't have this problem.
SELECT p.key, u.full_name
FROM project_members m
JOIN projects p ON p.id = m.project_id
JOIN users u ON u.id = m.user_id
WHERE m.user_id NOT IN (SELECT t.assignee_id
                        FROM tasks t
                        WHERE t.project_id = m.project_id)
ORDER BY p.key, u.full_name;


-- 5. Tasks with all given tags
-- name: tasks_with_all_tags
-- The app fills the 3 ?: the project id, the tag names (lower case, no repeats) as one array,
-- and how many names there are. Example: 1, {backend,urgent}, 2.
-- WHERE keeps only the task's links to tags in the list. GROUP BY gives one row per task,
-- and COUNT(*) is how many of the listed tags the task has. A task has each tag at most once,
-- so HAVING COUNT(*) = the list size keeps only the tasks that have every tag.
-- The list is one array parameter, so it can have any number of tags.
SELECT t.id,
       t.title
FROM tasks t
JOIN task_tags tt ON tt.task_id = t.id
JOIN tags g ON g.id = tt.tag_id
WHERE t.project_id = ? AND lower(g.name) = ANY (?)
GROUP BY t.id, t.title
HAVING COUNT(*) = ?
ORDER BY t.id;
