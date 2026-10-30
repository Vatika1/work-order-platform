ALTER TABLE clients
    ADD CONSTRAINT clients_status_chk CHECK (status IN ('ACTIVE', 'SUSPENDED'));