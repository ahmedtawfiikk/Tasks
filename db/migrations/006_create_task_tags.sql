CREATE TABLE task_tags (
  task_id BIGINT NOT NULL ,
  tag_id BIGINT NOT NULL ,
  project_id	BIGINT NOT NULL,
  PRIMARY KEY (task_id, tag_id),
  CONSTRAINT task_tags_task_fk FOREIGN KEY (task_id, project_id) REFERENCES tasks (id, project_id) ON DELETE CASCADE,
  CONSTRAINT task_tags_tag_fk FOREIGN KEY (tag_id, project_id) REFERENCES tags (id, project_id) ON DELETE CASCADE
);