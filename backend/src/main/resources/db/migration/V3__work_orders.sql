CREATE TABLE work_orders (
                             id           UUID PRIMARY KEY,
                             client_id    UUID NOT NULL REFERENCES clients(id),
                             title        VARCHAR(200) NOT NULL,
                             description  TEXT,
                             status       VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
                             created_by   UUID NOT NULL REFERENCES users(id),
                             created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             CONSTRAINT work_orders_status_chk CHECK (status IN
                                                                      ('DRAFT','SUBMITTED','IN_REVIEW','CHANGES_REQUESTED','APPROVED','IN_PROGRESS','COMPLETED','CANCELLED'))
);

CREATE INDEX idx_work_orders_client_status ON work_orders(client_id, status);