-- List without a status filter: WHERE client_id = ? ORDER BY created_at DESC
CREATE INDEX idx_work_orders_client_created
    ON work_orders (client_id, created_at DESC);

-- List with a status filter: WHERE client_id = ? AND status = ? ORDER BY created_at DESC
-- Also covers (client_id, status) lookups, so the old index is redundant
CREATE INDEX idx_work_orders_client_status_created
    ON work_orders (client_id, status, created_at DESC);

DROP INDEX IF EXISTS idx_work_orders_client_status;