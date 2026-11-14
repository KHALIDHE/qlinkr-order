-- Catalogue (categories and items, text in ar/fr/en) and scan points (where a QR code is placed).

CREATE TABLE scan_point (
    id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_id  NUMBER        NOT NULL,
    code         VARCHAR2(16)  NOT NULL,
    label        VARCHAR2(60)  NOT NULL,
    kind         VARCHAR2(10)  NOT NULL,
    active       BOOLEAN       DEFAULT TRUE NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_scan_point_business FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE CASCADE,
    CONSTRAINT uq_scan_point_code UNIQUE (code),
    CONSTRAINT ck_scan_point_kind CHECK (kind IN ('TABLE', 'COUNTER', 'SHELF', 'DOOR'))
);
CREATE INDEX ix_scan_point_business ON scan_point (business_id);

CREATE TABLE category (
    id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_id  NUMBER     NOT NULL,
    position     NUMBER(5)  DEFAULT 0 NOT NULL,
    CONSTRAINT fk_category_business FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE CASCADE
);
CREATE INDEX ix_category_business ON category (business_id, position);

CREATE TABLE category_i18n (
    category_id  NUMBER        NOT NULL,
    lang         VARCHAR2(2)   NOT NULL,
    name         VARCHAR2(80)  NOT NULL,
    CONSTRAINT pk_category_i18n PRIMARY KEY (category_id, lang),
    CONSTRAINT fk_category_i18n FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE,
    CONSTRAINT ck_category_i18n_lang CHECK (lang IN ('ar', 'fr', 'en'))
);

CREATE TABLE item (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_id   NUMBER         NOT NULL,
    category_id   NUMBER         NOT NULL,
    price_cents   NUMBER(10)     NOT NULL,
    photo_url     VARCHAR2(500),
    available     BOOLEAN        DEFAULT TRUE NOT NULL,
    stock         NUMBER(10),
    position      NUMBER(5)      DEFAULT 0 NOT NULL,
    version       NUMBER(19)     DEFAULT 0 NOT NULL,
    CONSTRAINT fk_item_business FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE,
    CONSTRAINT ck_item_price CHECK (price_cents >= 0),
    -- NULL stock means "not tracked"; a tracked stock can never go below zero.
    CONSTRAINT ck_item_stock CHECK (stock IS NULL OR stock >= 0)
);
CREATE INDEX ix_item_business_category ON item (business_id, category_id, position);

CREATE TABLE item_i18n (
    item_id      NUMBER          NOT NULL,
    lang         VARCHAR2(2)     NOT NULL,
    name         VARCHAR2(100)   NOT NULL,
    description  VARCHAR2(500),
    CONSTRAINT pk_item_i18n PRIMARY KEY (item_id, lang),
    CONSTRAINT fk_item_i18n FOREIGN KEY (item_id) REFERENCES item (id) ON DELETE CASCADE,
    CONSTRAINT ck_item_i18n_lang CHECK (lang IN ('ar', 'fr', 'en'))
);
