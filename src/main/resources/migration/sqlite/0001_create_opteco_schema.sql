CREATE TABLE IF NOT EXISTS "{{account_table}}" (
    uuid VARCHAR(36) NOT NULL PRIMARY KEY,
    player VARCHAR(64) NOT NULL,
    balance BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS "idx_{{account_table}}_balance"
    ON "{{account_table}}" (balance DESC);

CREATE TABLE IF NOT EXISTS "{{transaction_table}}" (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    sender VARCHAR(36) NOT NULL,
    receiver VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS "idx_{{transaction_table}}_state"
    ON "{{transaction_table}}" (state);
