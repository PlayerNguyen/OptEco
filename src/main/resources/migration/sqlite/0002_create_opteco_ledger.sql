CREATE TABLE IF NOT EXISTS "{{ledger_table}}" (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    player VARCHAR(36) NOT NULL,
    counterparty VARCHAR(36),
    amount BIGINT NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    reason VARCHAR(32) NOT NULL,
    transaction_id VARCHAR(36),
    created_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS "idx_{{ledger_table}}_player_created"
    ON "{{ledger_table}}" (player, created_at DESC);

CREATE INDEX IF NOT EXISTS "idx_{{ledger_table}}_created"
    ON "{{ledger_table}}" (created_at);
