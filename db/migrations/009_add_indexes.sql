-- Part 5: indexes for the 4 measured queries. Why each one exists, and the plans before and after,
-- are in docs/INDEXES.md.

-- The task list of one project, sorted by title, one page at a time (ORDER BY t.title, t.id LIMIT ? OFFSET ?).
-- The rows are already in that order inside the index, so the database reads only the first rows of the page.
CREATE INDEX tasks_project_title_id ON tasks (project_id, title, id);

-- Report 2: overdue tasks. Only tasks that are not done are in the index (WHERE: a partial index).
-- The entries are in the report's order (ORDER BY t.due_date, t.id), so there is no sort.
-- INCLUDE copies the other columns the report reads into the index, so the table is never read.
CREATE INDEX tasks_overdue ON tasks (due_date, id) INCLUDE (project_id, assignee_id, title) WHERE status <> 'done';

-- Report 3: top 3 people per project. The done tasks of one project, grouped by assignee.
-- The subquery runs once per person and project; it can count straight from the index.
CREATE INDEX tasks_done_project_assignee ON tasks (project_id, assignee_id) WHERE status = 'done';

-- Report 5: tasks with all given tags. The primary key (task_id, tag_id) finds the tags of a task;
-- this finds the tasks of a tag.
CREATE INDEX task_tags_tag_task ON task_tags (tag_id, task_id);
