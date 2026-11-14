-- Baseline for QlinkR Order.
-- Flyway records every migration in FLYWAY_SCHEMA_HISTORY. This first one
-- only creates a small key/value table used by the health endpoint, so we
-- can prove that migrations ran on a real Oracle before any business table exists.

CREATE TABLE app_meta (
    meta_key    VARCHAR2(64)  NOT NULL,
    meta_value  VARCHAR2(255) NOT NULL,
    updated_at  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_app_meta PRIMARY KEY (meta_key)
);

INSERT INTO app_meta (meta_key, meta_value) VALUES ('schema', 'qlinkr-order');
INSERT INTO app_meta (meta_key, meta_value) VALUES ('created_by', 'flyway');
