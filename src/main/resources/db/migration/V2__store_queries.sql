/***********************************
**  TABLES
************************************/

CREATE TABLE stored_query
(
    id               SERIAL PRIMARY KEY,
    structured_query TEXT      NOT NULL,
    label            TEXT      NOT NULL,
    comment          TEXT,
    last_modified    timestamp NOT NULL DEFAULT current_timestamp,
    created_by       TEXT      NOT NULL
);

/***********************************
**  OTHER CONSTRAINTS
************************************/

ALTER TABLE stored_query
    ADD CONSTRAINT stored_query_label_user_unique UNIQUE (label, created_by);
