package me.playernguyen.opteco.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;
import me.playernguyen.opteco.MoneyAssert;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.core.port.SystemClock;
import me.playernguyen.opteco.storage.jdbc.JdbcAccountRepository;
import me.playernguyen.opteco.storage.jdbc.SchemaMigrator;
import me.playernguyen.opteco.storage.jdbc.SqliteConnectionProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcAccountRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateAndMutateAccountsWhenUsingSQLite() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = new SqliteConnectionProvider(file("test.db"));
        OptEcoConfig config = TestConfigs.config();
        new SchemaMigrator(provider, config, Logger.getLogger("test")).migrate();
        JdbcAccountRepository repository =
                new JdbcAccountRepository(provider, config.accountTable(), SystemClock.INSTANCE);
        UUID uuid = UUID.randomUUID();

        // Act
        Account created = repository.getOrCreate(uuid, "Alice", BigDecimal.valueOf(100));
        BigDecimal credit;
        try (Connection connection = provider.getConnection()) {
            credit = repository
                    .credit(connection, uuid, "Alice", BigDecimal.valueOf(25))
                    .orElseThrow(AssertionError::new);
            BigDecimal debit = repository
                    .debit(connection, uuid, BigDecimal.valueOf(30), BigDecimal.valueOf(-10))
                    .orElseThrow(AssertionError::new);
            MoneyAssert.assertMoney("95", debit);
        }

        // Assert
        MoneyAssert.assertMoney("100", created.balance());
        MoneyAssert.assertMoney("125", credit);
        MoneyAssert.assertMoney(
                "95", repository.find(uuid).orElseThrow(AssertionError::new).balance());
        assertTrue(repository.exists(uuid));
        assertFalse(repository.exists(UUID.randomUUID()));
        provider.close();
    }

    @Test
    void shouldKeepTheHigherBalanceWhenMigratingDuplicateLegacyRows() throws Exception {
        // Arrange
        SqliteConnectionProvider provider = new SqliteConnectionProvider(file("legacy.db"));
        OptEcoConfig config = OptEcoConfig.builder()
                .useDouble(true)
                .accountTable("opteco")
                .transactionTable("opteco_transaction")
                .build();
        config.validate();
        UUID uuid = UUID.randomUUID();
        try (Connection connection = provider.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE opteco (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "player CHAR(255) NOT NULL, balance REAL NOT NULL, "
                    + "uuid CHAR(255) NOT NULL)");
            statement.execute("CREATE TABLE opteco_transaction (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "transaction_id VARCHAR NOT NULL, sender VARCHAR NOT NULL, "
                    + "receiver VARCHAR NOT NULL, amount REAL NOT NULL, "
                    + "state VARCHAR NOT NULL, time VARCHAR NOT NULL)");
        }
        try (Connection connection = provider.getConnection();
                PreparedStatement account =
                        connection.prepareStatement("INSERT INTO opteco (player, balance, uuid) VALUES (?, ?, ?)");
                PreparedStatement transaction = connection.prepareStatement("INSERT INTO opteco_transaction "
                        + "(transaction_id, sender, receiver, amount, state, time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            account.setString(1, "Alice");
            account.setDouble(2, 10.5);
            account.setString(3, uuid.toString());
            account.addBatch();
            account.setDouble(2, 20.5);
            account.addBatch();
            account.executeBatch();

            transaction.setString(1, "tx-1");
            transaction.setString(2, uuid.toString());
            transaction.setString(3, UUID.randomUUID().toString());
            transaction.setDouble(4, 5.0);
            transaction.setString(5, "PENDING");
            transaction.setString(6, "1000000");
            transaction.executeUpdate();
        }

        // Act
        new SchemaMigrator(provider, config, Logger.getLogger("test")).migrate();
        JdbcAccountRepository repository =
                new JdbcAccountRepository(provider, config.accountTable(), SystemClock.INSTANCE);

        // Assert
        MoneyAssert.assertMoney(
                "20.5", repository.find(uuid).orElseThrow(AssertionError::new).balance());
        provider.close();
    }

    private File file(String name) {
        return tempDir.resolve(name).toFile();
    }
}
