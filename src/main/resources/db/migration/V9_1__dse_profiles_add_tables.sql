CREATE TABLE IF NOT EXISTS dse_profile
(
    id    SERIAL PRIMARY KEY,
    url   TEXT UNIQUE NOT NULL,
    entry JSON        NOT NULL
);