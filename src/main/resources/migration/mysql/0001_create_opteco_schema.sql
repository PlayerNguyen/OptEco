CREATE TABLE IF NOT EXISTS `{{account_table}}` (
    uuid VARCHAR(36) NOT NULL,
    player VARCHAR(64) NOT NULL,
    balance BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (uuid),
    KEY `idx_{{account_table}}_balance` (balance)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `{{transaction_table}}` (
    id VARCHAR(36) NOT NULL,
    sender VARCHAR(36) NOT NULL,
    receiver VARCHAR(36) NOT NULL,
    amount BIGINT NOT NULL,
    state VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY `idx_{{transaction_table}}_state` (state)
) ENGINE=InnoDB;
