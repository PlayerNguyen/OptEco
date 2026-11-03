package me.playernguyen.opteco.storage.jdbc;

public enum SqlDialect {
    SQLITE {
        @Override
        public String quote(String identifier) {
            return '"' + identifier + '"';
        }

        @Override
        public String creditAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(uuid) DO UPDATE SET "
                    + "player = CASE WHEN excluded.player = '' THEN player ELSE excluded.player END, "
                    + "balance = balance + excluded.balance, "
                    + "updated_at = excluded.updated_at";
        }

        @Override
        public String setAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(uuid) DO UPDATE SET "
                    + "player = CASE WHEN excluded.player = '' THEN player ELSE excluded.player END, "
                    + "balance = excluded.balance, updated_at = excluded.updated_at";
        }

        @Override
        public String insertAccountIfAbsentSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(uuid) DO NOTHING";
        }

        @Override
        public String insertTransactionIgnoreSql(String table) {
            return "INSERT OR IGNORE INTO " + quote(table)
                    + " (id, sender, receiver, amount, state, created_at, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
    },
    MYSQL {
        @Override
        public String quote(String identifier) {
            return '`' + identifier + '`';
        }

        @Override
        public String creditAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "player = IF(VALUES(player) = '', player, VALUES(player)), "
                    + "balance = balance + VALUES(balance), updated_at = VALUES(updated_at)";
        }

        @Override
        public String setAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "player = IF(VALUES(player) = '', player, VALUES(player)), "
                    + "balance = VALUES(balance), updated_at = VALUES(updated_at)";
        }

        @Override
        public String insertAccountIfAbsentSql(String table) {
            return "INSERT IGNORE INTO " + quote(table) + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?)";
        }

        @Override
        public String insertTransactionIgnoreSql(String table) {
            return "INSERT IGNORE INTO " + quote(table)
                    + " (id, sender, receiver, amount, state, created_at, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
    };

    public abstract String quote(String identifier);

    public abstract String creditAccountSql(String table);

    public abstract String setAccountSql(String table);

    public abstract String insertAccountIfAbsentSql(String table);

    public abstract String insertTransactionIgnoreSql(String table);

    public String insertTransactionSql(String table) {
        return "INSERT INTO " + quote(table)
                + " (id, sender, receiver, amount, state, created_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }
}
