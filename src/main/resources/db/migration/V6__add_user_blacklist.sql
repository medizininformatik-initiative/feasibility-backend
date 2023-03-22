CREATE TABLE user_blacklist
(
    id             SERIAL PRIMARY KEY,
    user_id        TEXT UNIQUE NOT NULL,
    blacklisted_at timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
