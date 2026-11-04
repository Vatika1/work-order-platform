CREATE TABLE idempotency_keys (
                                  client_id        UUID         NOT NULL REFERENCES clients(id),
                                  idempotency_key  VARCHAR(100) NOT NULL,
                                  request_hash     VARCHAR(64)  NOT NULL,
                                  status           VARCHAR(20)  NOT NULL,          -- IN_PROGRESS / COMPLETED
                                  response_status  INT,
                                  response_body    TEXT,
                                  created_at       TIMESTAMPTZ  NOT NULL,
                                  expires_at       TIMESTAMPTZ  NOT NULL,
                                  PRIMARY KEY (client_id, idempotency_key)
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);