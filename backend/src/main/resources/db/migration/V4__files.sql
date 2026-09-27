CREATE TABLE file_asset (
                            id              UUID PRIMARY KEY,
                            client_id       UUID NOT NULL REFERENCES clients(id),
                            work_order_id   UUID NOT NULL REFERENCES work_orders(id),
                            file_name       VARCHAR(255) NOT NULL,
                            content_type    VARCHAR(100) NOT NULL,
                            uploaded_by     UUID NOT NULL REFERENCES users(id),
                            created_at      TIMESTAMPTZ NOT NULL,
                            updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_file_asset_client_work_order ON file_asset (client_id, work_order_id);

CREATE TABLE file_version (
                              id              UUID PRIMARY KEY,
                              file_asset_id   UUID NOT NULL REFERENCES file_asset(id),
                              version_number  INT  NOT NULL,
                              s3_key          VARCHAR(512) NOT NULL,
                              size_bytes      BIGINT NOT NULL,
                              created_at      TIMESTAMPTZ NOT NULL,
                              UNIQUE (file_asset_id, version_number)
);