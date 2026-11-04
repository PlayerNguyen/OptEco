package me.playernguyen.opteco.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import me.playernguyen.opteco.MoneyAssert;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.SystemClock;
import me.playernguyen.opteco.storage.jdbc.ConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.JdbcAccountRepository;
import me.playernguyen.opteco.storage.jdbc.JdbcPaymentRepository;
import me.playernguyen.opteco.storage.jdbc.JdbcUnitOfWork;
import me.playernguyen.opteco.storage.jdbc.MysqlConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.migration.SchemaMigrator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Integration coverage for the MySQL backend: the dialect-specific upserts, the
 * schema migration and the transaction boundaries.
 *
 * <p>The class is skipped unless {@code OPTECO_TEST_MYSQL_HOST} is set, so a
 * normal {@code ./gradlew build} never needs a database. CI runs it against a
 * {@code mysql:8} service container. Every run uses throwaway table names so
 * repeated or parallel runs cannot collide, and the tables are dropped
 * afterwards.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MysqlStorageTest {

    /** History table name fixed by {@link me.playernguyen.opteco.storage.jdbc.migration.MigrationHistory}. */
    private static final String HISTORY_TABLE = "opteco_schema_history";

    private ConnectionProvider provider;
    private OptEcoConfig config;
    private JdbcAccountRepository accounts;
    private JdbcPaymentRepository payments;
    private JdbcUnitOfWork unitOfWork;

    @BeforeAll
    void connectAndMigrate() throws Exception {
        // Arrange
        String configuredHost = environment("OPTECO_TEST_MYSQL_HOST");
        Assumptions.assumeTrue(
                configuredHost != null && !configuredHost.trim().isEmpty(),
                "Set OPTECO_TEST_MYSQL_HOST to run the MySQL integration tests");
        String host =
                Objects.requireNonNull(configuredHost, "OPTECO_TEST_MYSQL_HOST").trim();
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        config = OptEcoConfig.builder()
                .useDouble(true)
                .accountTable("opteco_it_" + suffix)
                .transactionTable("opteco_it_tx_" + suffix)
                .mysqlHost(host)
                .mysqlPort(port())
                .mysqlDatabase(environmentOrDefault("OPTECO_TEST_MYSQL_DATABASE", "opteco_test"))
                .mysqlUsername(environmentOrDefault("OPTECO_TEST_MYSQL_USER", "root"))
                .mysqlPassword(environmentOrDefault("OPTECO_TEST_MYSQL_PASSWORD", ""))
                .build();
        config.validate();
        provider = new MysqlConnectionProvider(config);

        // A shared history table would make the migrator skip the new tables.
        execute("DROP TABLE IF EXISTS `" + HISTORY_TABLE + "`");
        new SchemaMigrator(provider, config, Logger.getLogger("mysql-test")).migrate();
        accounts = new JdbcAccountRepository(provider, config.accountTable(), SystemClock.INSTANCE);
        payments = new JdbcPaymentRepository(provider, config.transactionTable());
        unitOfWork = new JdbcUnitOfWork(provider);
    }

    @AfterAll
    void dropThrowawayTables() {
        if (provider == null) {
            return;
        }
        try {
            execute("DROP TABLE IF EXISTS `" + config.accountTable() + "`");
            execute("DROP TABLE IF EXISTS `" + config.transactionTable() + "`");
            execute("DROP TABLE IF EXISTS `" + HISTORY_TABLE + "`");
        } catch (Exception ignored) {
            // Best-effort cleanup of a throwaway test database.
        } finally {
            provider.close();
        }
    }

    @Test
    void shouldRecordTheSchemaVersionWhenMigratingAFreshDatabase() throws Exception {
        // Act
        int applied = count("SELECT COUNT(*) FROM `" + HISTORY_TABLE + "`");

        // Assert
        assertEquals(1, applied, "A fresh MySQL database should record exactly one migration");
    }

    @Test
    void shouldCreditAndDebitAccountsOnMysql() throws Exception {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act
        BigDecimal credited;
        BigDecimal debited;
        try (Connection connection = provider.getConnection()) {
            accounts.getOrCreate(connection, uuid, "Alice", BigDecimal.TEN);
            credited = accounts.credit(connection, uuid, "Alice", BigDecimal.valueOf(25))
                    .orElseThrow(AssertionError::new);
            debited = accounts.debit(connection, uuid, BigDecimal.valueOf(5), BigDecimal.valueOf(-100))
                    .orElseThrow(AssertionError::new);
        }

        // Assert
        MoneyAssert.assertMoney("35", credited);
        MoneyAssert.assertMoney("30", debited);
        MoneyAssert.assertMoney(
                "30", accounts.find(uuid).orElseThrow(AssertionError::new).balance());
    }

    @Test
    void shouldKeepTheStoredNameWhenCreditingWithAnEmptyNameOnMysql() throws Exception {
        // Arrange
        UUID uuid = UUID.randomUUID();
        try (Connection connection = provider.getConnection()) {
            accounts.getOrCreate(connection, uuid, "Alice", BigDecimal.TEN);
        }

        // Act
        try (Connection connection = provider.getConnection()) {
            accounts.credit(connection, uuid, "", BigDecimal.ONE);
        }

        // Assert
        assertEquals(
                "Alice",
                accounts.find(uuid).orElseThrow(AssertionError::new).name(),
                "The MySQL upsert must keep the stored name when the incoming name is empty");
    }

    @Test
    void shouldReplaceTheBalanceWhenSettingAnAccountOnMysql() throws Exception {
        // Arrange
        UUID uuid = UUID.randomUUID();
        try (Connection connection = provider.getConnection()) {
            accounts.getOrCreate(connection, uuid, "Alice", BigDecimal.TEN);
        }

        // Act
        try (Connection connection = provider.getConnection()) {
            accounts.set(connection, uuid, "Alice", BigDecimal.valueOf(42));
        }

        // Assert
        MoneyAssert.assertMoney(
                "42", accounts.find(uuid).orElseThrow(AssertionError::new).balance());
    }

    @Test
    void shouldPersistAndTransitionPaymentsOnMysql() {
        // Arrange
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(12.5),
                TransactionState.PENDING,
                1000L,
                2000L);

        // Act
        payments.create(transaction);
        Transaction loaded = payments.find(transaction.id()).orElseThrow(AssertionError::new);
        boolean transitioned =
                payments.transition(transaction.id(), TransactionState.PENDING, TransactionState.CONFIRMED);

        // Assert
        assertEquals(transaction, loaded);
        assertTrue(transitioned);
        assertEquals(
                TransactionState.CONFIRMED,
                payments.find(transaction.id()).orElseThrow(AssertionError::new).state());
    }

    @Test
    void shouldRollBackAUnitOfWorkOnMysql() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        accounts.getOrCreate(uuid, "Rollback", BigDecimal.TEN);

        // Act
        assertThrows(
                IllegalStateException.class,
                () -> unitOfWork.execute(connection -> {
                    accounts.credit(connection, uuid, "Rollback", BigDecimal.ONE);
                    throw new IllegalStateException("boom");
                }));

        // Assert
        MoneyAssert.assertMoney(
                "10", accounts.find(uuid).orElseThrow(AssertionError::new).balance());
    }

    /**
     * Runs one statement against the test database.
     *
     * @param sql statement to execute
     * @throws Exception when the statement fails
     */
    private void execute(String sql) throws Exception {
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * Runs a single-column count query.
     *
     * @param sql query returning one integer
     * @return the value of the first column
     * @throws Exception when the query fails
     */
    private int count(String sql) throws Exception {
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static int port() {
        String value = environment("OPTECO_TEST_MYSQL_PORT");
        if (value == null || value.trim().isEmpty()) {
            return 3306;
        }
        return Integer.parseInt(value.trim());
    }

    private static String environment(String name) {
        return System.getenv(name);
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
