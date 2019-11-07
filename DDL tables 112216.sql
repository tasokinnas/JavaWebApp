
-- ALTER TABLE public.bugs DROP CONSTRAINT bugs_milestone_id_fkey;--
-- ALTER TABLE public.bugs DROP CONSTRAINT bugs_user_id_fkey;
-- ALTER TABLE public.comments DROP CONSTRAINT comments_bug_id_fkey;
-- ALTER TABLE public.comments DROP CONSTRAINT comments_user_id_fkey;
-- ALTER TABLE public.tags DROP CONSTRAINT tags_bug_id_fkey;
-- ALTER TABLE public.tags DROP CONSTRAINT tags_user_id_fkey;
-- ALTER TABLE public.users DROP CONSTRAINT users_bug_id_fkey;
-- ALTER TABLE public.users DROP CONSTRAINT users_tag_id_fkey;
-- ALTER TABLE public.bugs DROP CONSTRAINT bugs_pkey;
-- ALTER TABLE public.comments DROP CONSTRAINT comments_pkey;
-- ALTER TABLE public.milestones DROP CONSTRAINT milestones_pkey;
-- ALTER TABLE public.tags DROP CONSTRAINT tags_pkey;
-- ALTER TABLE public.users DROP CONSTRAINT users_pkey;
-- ALTER TABLE public.tag_bug_xref DROP CONSTRAINT pk_tag_bug_xref;
-- ALTER TABLE public.user_tag_subscription DROP CONSTRAINT pk_tag_user_xref;
-- ALTER TABLE public.tag_bug_xref DROP CONSTRAINT tag_bug_xref_bug_id_fkey;
--  ALTER TABLE public.user_tag_subscription DROP CONSTRAINT tag_bug_xref_tag_id_fkey;


DROP TABLE bugs cascade;
DROP TABLE comments cascade;
DROP TABLE milestones cascade;
DROP TABLE tags cascade;
DROP TABLE users cascade;
DROP TABLE tag_bug_xref cascade;
Drop Table user_tag_subscription cascade;

CREATE TABLE bugs
(
    bug_id SERIAL PRIMARY KEY NOT NULL,
    bug_title VARCHAR(100) NOT NULL,
    bug_body VARCHAR(2000) NOT NULL,
    bug_status VARCHAR(100) NOT NULL,
    create_date DATE NOT NULL,
    close_date DATE NULL,
    user_id INTEGER NOT NULL,
    milestone_id INTEGER NULL
);
CREATE TABLE comments
(
    comment_id SERIAL PRIMARY KEY NOT NULL,
    comment_body VARCHAR(100) NOT NULL,
    comment_date DATE NOT NULL,
    user_id INTEGER NOT NULL,
    bug_id INTEGER NOT NULL
);
CREATE TABLE milestones
(
    milestone_id SERIAL PRIMARY KEY NOT NULL,
    milestone_name VARCHAR(100) NOT NULL,
    milestone_description VARCHAR(100) NOT NULL
);
CREATE TABLE tags
(
    tag_id SERIAL PRIMARY KEY NOT NULL,
    tag VARCHAR(100) NOT NULL
);
CREATE TABLE tag_bug_xref
(
    tag_id INTEGER NOT NULL,
    bug_id INTEGER NOT NULL
);
CREATE TABLE user_tag_subscription
(
   user_id integer NOT NULL,
   tag_id integer NOT NULL
);
CREATE TABLE users
(
    user_id SERIAL PRIMARY KEY NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    user_email VARCHAR(100) NOT NULL,
    user_password VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    tag_id INTEGER,
    bug_id INTEGER
);

CREATE UNIQUE INDEX u_ix_username ON users (user_name);
CREATE UNIQUE INDEX u_ix_tag ON tags (tag);

ALTER TABLE user_tag_subscription ADD CONSTRAINT pk_tag_user_xref PRIMARY KEY (tag_id, user_id);
ALTER TABLE tag_bug_xref ADD CONSTRAINT pk_tag_bug_xref PRIMARY KEY (tag_id, bug_id);
ALTER TABLE tag_bug_xref ADD FOREIGN KEY (tag_id) REFERENCES tags (tag_id);
ALTER TABLE tag_bug_xref ADD FOREIGN KEY (bug_id) REFERENCES bugs (bug_id);
ALTER TABLE bugs ADD FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE bugs ADD FOREIGN KEY (milestone_id) REFERENCES milestones (milestone_id);
ALTER TABLE comments ADD FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE comments ADD FOREIGN KEY (bug_id) REFERENCES bugs (bug_id);
ALTER TABLE users ADD FOREIGN KEY (tag_id) REFERENCES tags (tag_id);
ALTER TABLE users ADD FOREIGN KEY (bug_id) REFERENCES bugs (bug_id);
ALTER TABLE user_tag_subscription ADD FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE user_tag_subscription ADD FOREIGN KEY (tag_id) REFERENCES tags (tag_id);
