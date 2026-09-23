CREATE TABLE clients (
                         id          UUID PRIMARY KEY,
                         name        VARCHAR(200) NOT NULL,
                         status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
                         created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
                       id            UUID PRIMARY KEY,
                       email         VARCHAR(320) NOT NULL UNIQUE,
                       password_hash VARCHAR(100) NOT NULL,
                       display_name  VARCHAR(200) NOT NULL,
                       role          VARCHAR(30)  NOT NULL,
                       client_id     UUID REFERENCES clients(id),
                       enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT users_role_chk CHECK (role IN ('CLIENT_USER','CLIENT_ADMIN','STAFF','REVIEWER','PLATFORM_ADMIN')),
                       CONSTRAINT users_client_chk CHECK (
                           (role IN ('CLIENT_USER','CLIENT_ADMIN') AND client_id IS NOT NULL) OR
                           (role IN ('STAFF','REVIEWER','PLATFORM_ADMIN') AND client_id IS NULL)
                           )
);

CREATE INDEX idx_users_client_id ON users(client_id);

CREATE TABLE refresh_tokens (
                                id          UUID PRIMARY KEY,
                                user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token_hash  VARCHAR(100) NOT NULL UNIQUE,
                                expires_at  TIMESTAMPTZ  NOT NULL,
                                revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);