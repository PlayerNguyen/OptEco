package me.playernguyen.opteco.storage.jdbc;

/**
 * SQL fragments that differ between the two supported backends, SQLite and MySQL.
 *
 * <p>Each constant knows how to quote identifiers and how to express an
 * "insert or update" for the account table and an "insert and ignore duplicates"
 * for the transaction table. The account upserts either add the incoming balance
 * to the stored value ({@link #creditAccountSql(String)}) or replace it
 * ({@link #setAccountSql(String)}), and both keep the stored player name when the
 * incoming name is empty. The table layout itself is owned by
 * {@link SchemaMigrator}.
 */
public enum SqlDialect {
    /**
     * SQLite flavour: double-quoted identifiers, {@code ON CONFLICT ... DO UPDATE}
     * upserts and {@code INSERT OR IGNORE} for duplicate-safe inserts.
     */
    SQLITE {
        /**
         * Quotes with double quotes, the SQLite identifier syntax.
         *
         * @param identifier the raw identifier
         * @return the identifier wrapped in double quotes
         */
        @Override
        public String quote(String identifier) {
            return '"' + identifier + '"';
        }

        /**
         * Builds the SQLite additive upsert.
         *
         * @param table the validated account table name
         * @return the {@code ON CONFLICT} statement that adds to the stored balance
         */
        @Override
        public String creditAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(uuid) DO UPDATE SET "
                    + "player = CASE WHEN excluded.player = '' THEN player ELSE excluded.player END, "
                    + "balance = balance + excluded.balance, "
                    + "updated_at = excluded.updated_at";
        }

        /**
         * Builds the SQLite replacing upsert.
         *
         * @param table the validated account table name
         * @return the {@code ON CONFLICT} statement that replaces the stored balance
         */
        @Override
        public String setAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(uuid) DO UPDATE SET "
                    + "player = CASE WHEN excluded.player = '' THEN player ELSE excluded.player END, "
                    + "balance = excluded.balance, updated_at = excluded.updated_at";
        }

        /**
         * Builds the SQLite insert that keeps an existing row untouched.
         *
         * @param table the validated account table name
         * @return the {@code ON CONFLICT ... DO NOTHING} statement
         */
        @Override
        public String insertAccountIfAbsentSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(uuid) DO NOTHING";
        }

        /**
         * Builds the SQLite duplicate-safe payment insert.
         *
         * @param table the validated transaction table name
         * @return the {@code INSERT OR IGNORE} statement
         */
        @Override
        public String insertTransactionIgnoreSql(String table) {
            return "INSERT OR IGNORE INTO " + quote(table)
                    + " (id, sender, receiver, amount, state, created_at, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
    },
    /**
     * MySQL flavour: backtick identifiers, {@code ON DUPLICATE KEY UPDATE}
     * upserts and {@code INSERT IGNORE} for duplicate-safe inserts.
     */
    MYSQL {
        /**
         * Quotes with backticks, the MySQL identifier syntax.
         *
         * @param identifier the raw identifier
         * @return the identifier wrapped in backticks
         */
        @Override
        public String quote(String identifier) {
            return '`' + identifier + '`';
        }

        /**
         * Builds the MySQL additive upsert.
         *
         * @param table the validated account table name
         * @return the {@code ON DUPLICATE KEY UPDATE} statement that adds to the
         *         stored balance
         */
        @Override
        public String creditAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "player = IF(VALUES(player) = '', player, VALUES(player)), "
                    + "balance = balance + VALUES(balance), updated_at = VALUES(updated_at)";
        }

        /**
         * Builds the MySQL replacing upsert.
         *
         * @param table the validated account table name
         * @return the {@code ON DUPLICATE KEY UPDATE} statement that replaces the
         *         stored balance
         */
        @Override
        public String setAccountSql(String table) {
            return "INSERT INTO " + quote(table)
                    + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "player = IF(VALUES(player) = '', player, VALUES(player)), "
                    + "balance = VALUES(balance), updated_at = VALUES(updated_at)";
        }

        /**
         * Builds the MySQL insert that keeps an existing row untouched.
         *
         * @param table the validated account table name
         * @return the {@code INSERT IGNORE} statement
         */
        @Override
        public String insertAccountIfAbsentSql(String table) {
            return "INSERT IGNORE INTO " + quote(table) + " (uuid, player, balance, updated_at) VALUES (?, ?, ?, ?)";
        }

        /**
         * Builds the MySQL duplicate-safe payment insert.
         *
         * @param table the validated transaction table name
         * @return the {@code INSERT IGNORE} statement
         */
        @Override
        public String insertTransactionIgnoreSql(String table) {
            return "INSERT IGNORE INTO " + quote(table)
                    + " (id, sender, receiver, amount, state, created_at, expires_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        }
    };

    /**
     * Quotes a table or column identifier so reserved words and mixed case are
     * safe.
     *
     * @param identifier a table or column name already validated as an SQL identifier
     * @return the identifier wrapped in this dialect's quote characters
     */
    public abstract String quote(String identifier);

    /**
     * Builds the upsert that creates an account row or adds the bound amount to
     * the stored balance.
     *
     * <p>Bind order is {@code uuid, player, balance, updated_at}. An empty bound
     * player name preserves the name already stored for the account.
     *
     * @param table the validated account table name
     * @return the statement with four placeholders
     */
    public abstract String creditAccountSql(String table);

    /**
     * Builds the upsert that creates an account row or replaces the stored
     * balance with the bound value.
     *
     * <p>Bind order is {@code uuid, player, balance, updated_at}. An empty bound
     * player name preserves the name already stored for the account.
     *
     * @param table the validated account table name
     * @return the statement with four placeholders
     */
    public abstract String setAccountSql(String table);

    /**
     * Builds a statement that inserts a zero or initial balance only when the
     * uuid is unknown, leaving an existing balance untouched.
     *
     * @param table the validated account table name
     * @return the statement with four placeholders
     */
    public abstract String insertAccountIfAbsentSql(String table);

    /**
     * Builds a duplicate-safe insert for a payment row. Legacy imports use it so
     * that re-running a migration cannot duplicate an already copied payment.
     *
     * @param table the validated transaction table name
     * @return the statement with seven placeholders
     */
    public abstract String insertTransactionIgnoreSql(String table);

    /**
     * Builds the plain insert used when a duplicate payment id must fail instead
     * of being ignored.
     *
     * @param table the validated transaction table name
     * @return the statement with seven placeholders
     */
    public String insertTransactionSql(String table) {
        return "INSERT INTO " + quote(table)
                + " (id, sender, receiver, amount, state, created_at, expires_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }
}
