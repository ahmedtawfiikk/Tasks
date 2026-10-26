CREATE TABLE project_members (
  
  project_id BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  role TEXT NOT NULL ,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT project_members_role_valid CHECK (role IN ('owner', 'maintainer', 'member')),
  PRIMARY KEY (project_id, user_id),
  CONSTRAINT project_members_project_user_role_key UNIQUE (project_id, user_id, role)
  
);

CREATE UNIQUE INDEX project_members_one_owner ON project_members (project_id) WHERE role = 'owner';