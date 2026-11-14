-- Indexes for the owner dashboard. Plans before and after are in docs/sql-notes.md.
--
-- Every dashboard query filters on one business and a date range. This index
-- serves that range scan directly, and also carries status and total, so the
-- sales query can be answered from the index alone without visiting the table.
CREATE INDEX ix_order_business_created ON customer_order (business_id, created_at, status, total_cents);

-- Best items joins order lines by order id; ix_order_line_order (V4) already covers it.
