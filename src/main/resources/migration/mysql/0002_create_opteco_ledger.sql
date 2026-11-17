CREATE TABLE IF NOT EXISTS `{{ledger_table}}` (
    id VARCHAR(36) NOT NULL,
    player VARCHAR(36) NOT NULL,
    counterparty VARCHAR(36),
    amount BIGINT NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    reason VARCHAR(32) NOT NULL,
    transaction_id VARCHAR(36),
    created_at BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY `idx_{{ledger_table}}_player_created` (player, created_at),
    KEY `idx_{{ledger_table}}_created` (created_at)
) ENGINE=InnoDB;
