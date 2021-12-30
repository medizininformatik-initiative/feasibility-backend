/***********************************
**  TYPES
************************************/

CREATE TYPE result_type AS ENUM (
    'SUCCESS',
    'ERROR'
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

CREATE TABLE site (
    id SERIAL PRIMARY KEY,
    site_name TEXT NOT NULL,
    aktin_identifier TEXT,
    dsf_identifier TEXT
);

CREATE TABLE result (
    query_id INTEGER NOT NULL,
    site_id INTEGER NOT NULL,
    result_type result_type NOT NULL DEFAULT 'SUCCESS',
    result INTEGER,
    received_at timestamp NOT NULL DEFAULT current_timestamp,
    display_site_id INTEGER NOT NULL
);

/***********************************
**  COMPOSITE PRIMARY KEYS
************************************/

ALTER TABLE result
    ADD CONSTRAINT result_pkey PRIMARY KEY (query_id, site_id);

/***********************************
**  FOREIGN KEY RELATIONS
************************************/

ALTER TABLE query
    ADD CONSTRAINT query_query_content_id_fkey FOREIGN KEY (query_content_id) REFERENCES query_content (id) ON DELETE CASCADE;

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

ALTER TABLE site
    ADD CONSTRAINT site_aktin_identifier_unique UNIQUE (aktin_identifier);

ALTER TABLE site
    ADD CONSTRAINT site_dsf_identifier_unique UNIQUE (dsf_identifier);

ALTER TABLE result
    ADD CONSTRAINT result_display_site_unique UNIQUE (query_id, display_site_id);

/***********************************
**  TRIGGERS AND FUNCTIONS
************************************/

-- TODO: discuss whether this raises problems (maybe we generate hashes in a different fashion, e.g. for lookups etc.)

-- CREATE OR REPLACE FUNCTION query_content_generate_hash() RETURNS trigger AS
-- $query_content_generate_hash$
-- BEGIN
--     NEW.hash := MD5(NEW.query_content);
--     RETURN NEW;
-- END;
-- $query_content_generate_hash$ LANGUAGE plpgsql;
--
-- CREATE TRIGGER query_content_generate_hash BEFORE INSERT ON query_content FOR EACH ROW EXECUTE PROCEDURE query_content_generate_hash();