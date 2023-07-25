CREATE TABLE IF NOT EXISTS termcode
(
    id      SERIAL PRIMARY KEY,
    system  TEXT NOT NULL,
    code    TEXT NOT NULL,
    version TEXT,
    unique nulls not DISTINCT (system, code, version),
    display TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS ui_profile
(
    id         SERIAL PRIMARY KEY,
    name       TEXT NOT NULL,
    ui_profile JSON NOT NULL
);

CREATE TABLE IF NOT EXISTS context
(
    id      SERIAL PRIMARY KEY,
    system  TEXT NOT NULL,
    code    TEXT NOT NULL,
    version TEXT,
    UNIQUE NULLS NOT DISTINCT (system, code, version),
    display TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS mapping
(
    id      SERIAL PRIMARY KEY,
    name    TEXT NOT NULL,
    type    TEXT NOT NULL,
    UNIQUE (name, type),
    content JSON NOT NULL
);

CREATE TABLE IF NOT EXISTS contextualized_termcode
(
    context_termcode_hash TEXT PRIMARY KEY,
    context_id            INTEGER NOT NULL,
    termcode_id           INTEGER NOT NULL,
    mapping_id            INTEGER,
    ui_profile_id         INTEGER,
    CONSTRAINT CONTEXT_ID_FK FOREIGN KEY (context_id)
        REFERENCES CONTEXT (id) ON DELETE CASCADE,
    CONSTRAINT CONCEPT_ID_FK FOREIGN KEY (termcode_id)
        REFERENCES termcode (id) ON DELETE CASCADE,
    CONSTRAINT mapping_id_fk FOREIGN KEY (mapping_id)
        REFERENCES mapping (id),
    CONSTRAINT ui_profile_id_fk FOREIGN KEY (ui_profile_id)
        REFERENCES ui_profile (id),
    UNIQUE (context_id, termcode_id)
);

CREATE TABLE IF NOT EXISTS criteria_set
(
    id  SERIAL PRIMARY KEY,
    url TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS contextualized_termcode_to_criteria_set
(
    context_termcode_hash       TEXT    NOT NULL,
    criteria_set_id INTEGER NOT NULL,
    UNIQUE (context_termcode_hash, criteria_set_id),
    CONSTRAINT CRITERIA_SET_ID_FK FOREIGN KEY (criteria_set_id)
        REFERENCES CRITERIA_SET (id) ON DELETE CASCADE,
    CONSTRAINT CONTEXTUALIZED_TERMCODE_ID_FK FOREIGN KEY (context_termcode_hash)
        REFERENCES CONTEXTUALIZED_TERMCODE (context_termcode_hash) ON DELETE CASCADE
);

CREATE INDEX idx_mapping_name_mapping ON mapping (name);
CREATE INDEX idx_contextualized_termcode_termcode_fk ON contextualized_termcode (termcode_id);

COMMENT ON COLUMN contextualized_termcode.context_termcode_hash IS 'This value is hashed using UUID-V3 (MD5) from the concatenated string of (context.system, context.code, context.version, termcode.system, termcode.code, termcode.version), omitting null values for version and without any delimiters. The mandatory namespace UUID will be predefined and shared along all components. The conversion between chars and bytes for hashing is using UTF-8 encoding.';
