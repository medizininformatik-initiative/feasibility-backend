/***********************************
**  TYPES
************************************/

CREATE TYPE result_type AS ENUM (
    'SUCCESS',
    'ERROR'
);

CREATE TYPE broker_type AS ENUM (
    'AKTIN',
    'DIRECT',
    'DSF',
    'MOCK'
);

/***********************************
**  TABLES
************************************/

CREATE TABLE query (
    id SERIAL PRIMARY KEY,
    query_content_id INTEGER,
    created_at timestamp NOT NULL DEFAULT current_timestamp
);

CREATE TABLE query_content (
    id SERIAL PRIMARY KEY,
    query_content TEXT NOT NULL,
    hash TEXT
);

CREATE TABLE query_dispatch (
    query_id INTEGER NOT NULL,
    external_query_id TEXT NOT NULL,
    broker_type broker_type NOT NULL,
    dispatched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE site (
    id SERIAL PRIMARY KEY,
    site_name TEXT NOT NULL
);

CREATE TABLE result (
    id SERIAL PRIMARY KEY,
    query_id INTEGER NOT NULL,
    site_id INTEGER NOT NULL,
    result_type result_type NOT NULL DEFAULT 'SUCCESS',
    result INTEGER,
    received_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

/***********************************
**  COMPOSITE PRIMARY KEYS
************************************/

ALTER TABLE query_dispatch
    ADD CONSTRAINT query_dispatch_pkey PRIMARY KEY (query_id, external_query_id, broker_type);


/***********************************
**  FOREIGN KEY RELATIONS
************************************/

ALTER TABLE query
    ADD CONSTRAINT query_query_content_id_fkey FOREIGN KEY (query_content_id) REFERENCES query_content (id) ON DELETE CASCADE;

ALTER TABLE query_dispatch
    ADD CONSTRAINT query_dispatch_query_id_fkey FOREIGN KEY (query_id) REFERENCES query (id) ON DELETE CASCADE;

ALTER TABLE result
    ADD CONSTRAINT result_query_id_fkey FOREIGN KEY (query_id) REFERENCES query (id) ON DELETE CASCADE;
ALTER TABLE result
    ADD CONSTRAINT result_site_id_fkey FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE;

/***********************************
**  OTHER CONSTRAINTS
************************************/

ALTER TABLE query_content
    ADD CONSTRAINT query_hash_unique UNIQUE(hash);

ALTER TABLE site
    ADD CONSTRAINT site_name_unique UNIQUE (site_name);
