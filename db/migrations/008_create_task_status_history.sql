CREATE TABLE task_status_history (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_id BIGINT NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
  old_status task_status NOT NULL,
  new_status task_status NOT NULL,
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  changed_by BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  CONSTRAINT task_status_history_status_changed CHECK (old_status <> new_status)
);
