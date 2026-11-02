CREATE TYPE member_role AS ENUM ('owner', 'maintainer', 'member');

CREATE TABLE project_members (
  project_id BIGINT NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
  -- RESTRICT: a user can't be deleted while still in a project, so an owner can't vanish with their user.
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  role member_role NOT NULL,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (project_id, user_id)
);

-- At most one owner per project.
CREATE UNIQUE INDEX project_members_one_owner ON project_members (project_id) WHERE role = 'owner';
