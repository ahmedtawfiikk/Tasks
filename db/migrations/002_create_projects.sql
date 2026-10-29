-- projects table handeled the requirements 
CREATE TABLE projects (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  key TEXT NOT NULL UNIQUE,
  owner_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
  owner_role TEXT NOT NULL DEFAULT 'owner',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT projects_name_not_blank CHECK (btrim(name) <> ''),
  CONSTRAINT projects_key_format CHECK (key ~ '^[A-Z]{2,5}$'),
CONSTRAINT projects_owner_role_is_owner CHECK (owner_role = 'owner')
);