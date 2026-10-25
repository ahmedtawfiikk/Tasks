-- users table
CREATE TABLE users (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  full_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT users_full_name_not_blank CHECK (btrim(full_name) <> ''),
  CONSTRAINT users_email_not_blank CHECK (btrim(email) <> '')
);
  CREATE UNIQUE INDEX users_email_lower_key  ON users (lower(email));