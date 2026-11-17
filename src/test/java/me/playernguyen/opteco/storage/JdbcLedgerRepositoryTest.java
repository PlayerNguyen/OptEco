package me.playernguyen.opteco.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import me.playernguyen.opteco.MoneyAssert;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.LedgerEntry;
import me.playernguyen.opteco.storage.jdbc.JdbcLedgerRepository;
import me.playernguyen.opteco.storage.jdbc.SqliteConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.migration.SchemaMigrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcLedgerRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAppendAndPagePlayerEntriesNewestFirstWhenUsingSQLite() throws Exception {
        // Arrange
        SqliteConnectionProvider provider =
                new SqliteConnectionProvider(tempDir.resolve("ledger.db").toFile());
        OptEcoConfig config = TestConfigs.config();
        new SchemaMigrator(provider, config, Logger.getLogger("test")).migrate();
        JdbcLedgerRepository repository = new JdbcLedgerRepository(provider, config.ledgerTable());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        LedgerEntry first =
                entry("entry-1", alice, null, "10", "100", "110", BalanceChangeReason.ADMIN_ADD, null, 1000L);
        LedgerEntry second =
                entry("entry-2", alice, null, "-5", "110", "105", BalanceChangeReason.ADMIN_TAKE, null, 2000L);
        LedgerEntry third =
                entry("entry-3", alice, bob, "25", "105", "130", BalanceChangeReason.PAYMENT_RECEIVED, "tx-1", 3000L);
        LedgerEntry other = entry("entry-4", bob, alice, "1", "0", "1", BalanceChangeReason.ADMIN_ADD, null, 4000L);

        // Act
        try (Connection connection = provider.getConnection()) {
            repository.append(connection, first);
            repository.append(connection, second);
            repository.append(connection, third);
            repository.append(connection, other);
        }
        List<LedgerEntry> newestPage = repository.findByPlayer(alice, 2, 0);
        List<LedgerEntry> lastPage = repository.findByPlayer(alice, 2, 2);
        long aliceCount = repository.countByPlayer(alice);
        long bobCount = repository.countByPlayer(bob);

        // Assert
        assertEquals(2, newestPage.size());
        assertEquals(third, newestPage.get(0));
        assertEquals(second, newestPage.get(1));
        assertEquals(1, lastPage.size());
        assertEquals(first, lastPage.get(0));
        assertEquals(3L, aliceCount);
        assertEquals(1L, bobCount);
        MoneyAssert.assertMoney("25", third.amount());
        assertEquals(bob, third.counterparty());
        assertEquals("tx-1", third.transactionId());
        assertNull(second.counterparty());
        provider.close();
    }

    @Test
    void shouldUseConfiguredLedgerTableWhenCustomNameIsSet() throws Exception {
        // Arrange
        SqliteConnectionProvider provider =
                new SqliteConnectionProvider(tempDir.resolve("custom.db").toFile());
        OptEcoConfig config = OptEcoConfig.builder()
                .useDouble(true)
                .ledgerTable("custom_ledger")
                .build();
        config.validate();
        new SchemaMigrator(provider, config, Logger.getLogger("test")).migrate();
        JdbcLedgerRepository repository = new JdbcLedgerRepository(provider, config.ledgerTable());
        UUID player = UUID.randomUUID();
        LedgerEntry entry =
                entry("custom-entry", player, null, "7", "0", "7", BalanceChangeReason.API_ADD, null, 5000L);

        // Act
        try (Connection connection = provider.getConnection()) {
            repository.append(connection, entry);
        }

        // Assert
        assertEquals(1L, repository.countByPlayer(player));
        assertEquals(entry, repository.findByPlayer(player, 10, 0).get(0));
        provider.close();
    }

    private LedgerEntry entry(
            String id,
            UUID player,
            UUID counterparty,
            String amount,
            String before,
            String after,
            BalanceChangeReason reason,
            String transactionId,
            long createdAt) {
        return new LedgerEntry(
                id,
                player,
                counterparty,
                new BigDecimal(amount),
                new BigDecimal(before),
                new BigDecimal(after),
                reason,
                transactionId,
                createdAt);
    }
}
