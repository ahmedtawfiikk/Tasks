CREATE TABLE tags (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  project_id	BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT tags_name_not_blank CHECK (btrim(name) <> ''),
  CONSTRAINT tags_project_id_key UNIQUE (id,project_id)
);
CREATE UNIQUE INDEX tags_project_name_lower_key ON tags (project_id, lower(name));