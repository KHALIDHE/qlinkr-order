-- Orders placed from a phone after scanning, and their status history.

CREATE TABLE customer_order (
    id               NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_id      NUMBER         NOT NULL,
    scan_point_id    NUMBER         NOT NULL,
    kind             VARCHAR2(12)   NOT NULL,
    status           VARCHAR2(12)   NOT NULL,
    idempotency_key  VARCHAR2(64)   NOT NULL,
    public_token     VARCHAR2(32)   NOT NULL,
    total_cents      NUMBER(12)     NOT NULL,
    customer_note    VARCHAR2(200),
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    accepted_at      TIMESTAMP(6) WITH TIME ZONE,
    ready_at         TIMESTAMP(6) WITH TIME ZONE,
    completed_at     TIMESTAMP(6) WITH TIME ZONE,
    version          NUMBER(19)     DEFAULT 0 NOT NULL,
    CONSTRAINT fk_order_business FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_scan_point FOREIGN KEY (scan_point_id) REFERENCES scan_point (id),
    -- A double tap or a retried request carries the same key: the database itself refuses a second order.
    CONSTRAINT uq_order_idempotency UNIQUE (business_id, idempotency_key),
    CONSTRAINT uq_order_public_token UNIQUE (public_token),
    CONSTRAINT ck_order_kind CHECK (kind IN ('ORDER', 'CALL_ME', 'BOOKING')),
    CONSTRAINT ck_order_status CHECK (status IN ('NEW', 'ACCEPTED', 'PREPARING', 'READY', 'COMPLETED', 'CANCELLED', 'REJECTED'))
);
CREATE INDEX ix_order_business_status ON customer_order (business_id, status, created_at);

CREATE TABLE order_line (
    id                NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id          NUMBER        NOT NULL,
    item_id           NUMBER        NOT NULL,
    item_name         VARCHAR2(100) NOT NULL,
    quantity          NUMBER(4)     NOT NULL,
    unit_price_cents  NUMBER(10)    NOT NULL,
    CONSTRAINT fk_order_line_order FOREIGN KEY (order_id) REFERENCES customer_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_line_item FOREIGN KEY (item_id) REFERENCES item (id),
    CONSTRAINT ck_order_line_quantity CHECK (quantity BETWEEN 1 AND 50)
);
CREATE INDEX ix_order_line_order ON order_line (order_id);

CREATE TABLE order_event (
    id             NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id       NUMBER        NOT NULL,
    from_status    VARCHAR2(12),
    to_status      VARCHAR2(12)  NOT NULL,
    actor_user_id  NUMBER,
    at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_order_event_order FOREIGN KEY (order_id) REFERENCES customer_order (id) ON DELETE CASCADE
);
CREATE INDEX ix_order_event_order ON order_event (order_id, at);
