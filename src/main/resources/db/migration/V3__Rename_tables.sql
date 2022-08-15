/***********************************
**  TABLES
************************************/

DROP TABLE stored_query;

-- This should be non null...but how to deal with legacy data?
ALTER TABLE query
    ADD COLUMN created_by text NOT NULL DEFAULT 'unknown';

CREATE TABLE saved_query
(
    ID       SERIAL PRIMARY KEY,
    query_id INTEGER UNIQUE NOT NULL,
    deleted  boolean DEFAULT FALSE,
    label    text           NOT NULL,
    comment  text
);

CREATE TABLE query_template
(
    id            SERIAL PRIMARY KEY,
    query_id      INTEGER   NOT NULL,
    label         TEXT      NOT NULL,
    comment       TEXT,
    last_modified timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/***********************************
**  FOREIGN KEY RELATIONS
************************************/

ALTER TABLE saved_query
    ADD CONSTRAINT saved_query_query_id_fkey FOREIGN KEY (query_id) REFERENCES query (id) ON DELETE CASCADE;

ALTER TABLE query_template
    ADD CONSTRAINT query_template_query_id_fkey FOREIGN KEY (query_id) REFERENCES query (id) ON DELETE CASCADE;

