--SQL DDL for bug tracker database setup

CREATE TABLE users (
  user_id SERIAL PRIMARY KEY,
  user_name VARCHAR(100) NOT NULL,
  user_email VARCHAR(100) NOT NULL,
  user_password VARCHAR(100) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  bug_id INTEGER NOT NULL REFERENCES bugs,
  tag_id INTEGER NOT NULL REFERENCES tags
);

CREATE TABLE bugs (
  bug_id SERIAL PRIMARY KEY,
  bug_title VARCHAR(100) NOT NULL,
  bug_body VARCHAR(100) NOT NULL,
  bug_status VARCHAR(100) NOT NULL,
  create_date DATE NOT NULL,
  close_date DATE NOT NULL,
  user_id INTEGER NOT NULL REFERENCES users,
  milestone_id INTEGER NOT NULL REFERENCES milstones,
  tag_id INTEGER NOT NULL REFERENCES tags
);

CREATE TABLE tags (
  tag_id SERIAL PRIMARY KEY,
  tag_name VARCHAR(100) NOT NULL,
  user_id INTEGER NOT NULL REFERENCES users,
  bug_id INTEGER NOT NULL REFERENCES bugs
);

CREATE TABLE comments (
  comment_id SERIAL PRIMARY KEY,
  comment_body VARCHAR(100) NOT NULL,
  comment_date DATE NOT NULL,
  user_id INTEGER NOT NULL REFERENCES users,
  bug_id INTEGER NOT NULL REFERENCES bugs
);

CREATE TABLE milestones (
  milestone_id SERIAL PRIMARY KEY,
  milestone_name VARCHAR(100) NOT NULL,
  milestone_description VARCHAR(100) NOT NULL,
  bug_id INTEGER NOT NULL REFERENCES bugs
);