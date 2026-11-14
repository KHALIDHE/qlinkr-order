-- Businesses (tenants) and the people who log in to manage them.

CREATE TABLE business (
    id             NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           VARCHAR2(120) NOT NULL,
    slug           VARCHAR2(80)  NOT NULL,
    business_type  VARCHAR2(20)  NOT NULL,
    default_lang   VARCHAR2(2)   DEFAULT 'fr' NOT NULL,
    theme          VARCHAR2(20)  DEFAULT 'simple' NOT NULL,
    created_at     TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uq_business_slug UNIQUE (slug),
    CONSTRAINT ck_business_type CHECK (business_type IN ('RESTAURANT', 'SHOP', 'SERVICE')),
    CONSTRAINT ck_business_lang CHECK (default_lang IN ('ar', 'fr', 'en'))
);

CREATE TABLE app_user (
    id             NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_id    NUMBER,
    email          VARCHAR2(160) NOT NULL,
    password_hash  VARCHAR2(100) NOT NULL,
    role           VARCHAR2(20)  NOT NULL,
    active         BOOLEAN       DEFAULT TRUE NOT NULL,
    created_at     TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_app_user_business FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE CASCADE,
    CONSTRAINT ck_app_user_role CHECK (role IN ('PLATFORM_ADMIN', 'OWNER', 'STAFF')),
    -- A platform admin belongs to no business; everyone else belongs to exactly one.
    CONSTRAINT ck_app_user_scope CHECK (
        (role = 'PLATFORM_ADMIN' AND business_id IS NULL)
        OR (role <> 'PLATFORM_ADMIN' AND business_id IS NOT NULL))
);

-- Function-based unique index: emails are unique whatever their case.
CREATE UNIQUE INDEX ux_app_user_email ON app_user (LOWER(email));
CREATE INDEX ix_app_user_business ON app_user (business_id);
