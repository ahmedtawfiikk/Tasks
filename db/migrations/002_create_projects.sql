-- projects table handeled the requirements 
-- member_role is created here because projects.owner_role is its first user.
CREATE TYPE member_role AS ENUM ('owner', 'maintainer', 'member');

CREATE TABLE projects (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  key TEXT NOT NULL UNIQUE,
  owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  owner_role member_role NOT NULL DEFAULT 'owner',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT projects_name_not_blank CHECK (btrim(name) <> ''),
  CONSTRAINT projects_key_format CHECK (key ~ '^[A-Z]{2,5}$'),
  CONSTRAINT projects_owner_role_is_owner CHECK (owner_role = 'owner')
);
