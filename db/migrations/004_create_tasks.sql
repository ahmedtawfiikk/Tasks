CREATE TYPE task_status AS ENUM ('pending', 'in_progress', 'done');
CREATE TYPE task_type AS ENUM ('bug', 'feature');
CREATE TYPE bug_severity AS ENUM ('low', 'medium', 'high', 'critical');

CREATE TABLE tasks (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  status task_status NOT NULL DEFAULT 'pending',
  reporter_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  assignee_id BIGINT,
  due_date DATE,
  completed_at TIMESTAMPTZ,
  severity bug_severity,
  estimate_hours numeric(5, 2),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  type task_type NOT NULL,

  CONSTRAINT tasks_title_not_blank         CHECK (btrim(title) <> ''),
  CONSTRAINT tasks_estimate_range          CHECK (estimate_hours > 0 AND estimate_hours <= 200),
  CONSTRAINT tasks_type_fields             CHECK ((type = 'bug' AND severity IS NOT NULL AND estimate_hours IS NULL) OR (type = 'feature' AND estimate_hours IS NOT NULL AND severity IS NULL)),
  CONSTRAINT tasks_completed_iff_done      CHECK ((completed_at IS NOT NULL) = (status = 'done')),
  CONSTRAINT tasks_completed_after_created CHECK (completed_at >= created_at),
  CONSTRAINT tasks_id_project_key          UNIQUE (id, project_id),
  CONSTRAINT tasks_assignee_is_member FOREIGN KEY (project_id, assignee_id)
    REFERENCES project_members (project_id, user_id)
    ON DELETE SET NULL (assignee_id)
);
