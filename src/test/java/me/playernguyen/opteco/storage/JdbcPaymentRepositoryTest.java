package me.playernguyen.opteco.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.config.OptEcoConfig;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.storage.jdbc.JdbcPaymentRepository;
import me.playernguyen.opteco.storage.jdbc.SqliteConnectionProvider;
import me.playernguyen.opteco.storage.jdbc.migration.SchemaMigrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JdbcPaymentRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistAndTransitionPaymentWhenUsingSQLite() {
        // Arrange
        SqliteConnectionProvider provider =
                new SqliteConnectionProvider(tempDir.resolve("payments.db").toFile());
        OptEcoConfig config = TestConfigs.config();
        new SchemaMigrator(provider, config, Logger.getLogger("test")).migrate();
        JdbcPaymentRepository repository = new JdbcPaymentRepository(provider, config.transactionTable());
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                sender,
                receiver,
                BigDecimal.valueOf(12.5),
                TransactionState.PENDING,
                1000L,
                2000L);

        // Act
        repository.create(transaction);
        Transaction loaded = repository.find(transaction.id()).orElseThrow(AssertionError::new);
        boolean transitioned =
                repository.transition(transaction.id(), TransactionState.PENDING, TransactionState.CONFIRMED);

        // Assert
        assertEquals(transaction, loaded);
        assertTrue(transitioned);
        assertEquals(
                TransactionState.CONFIRMED,
                repository
                        .find(transaction.id())
                        .orElseThrow(AssertionError::new)
                        .state());
        assertFalse(repository.findPendingBySender(sender).isPresent());
        assertEquals(1, repository.findRecentBySender(sender, 10).size());
        provider.close();
    }

    @Test
    void shouldExpireOverduePendingTransactions() {
        // Arrange
        SqliteConnectionProvider provider =
                new SqliteConnectionProvider(tempDir.resolve("expire.db").toFile());
        OptEcoConfig config = TestConfigs.config();
        new SchemaMigrator(provider, config, Logger.getLogger("test")).migrate();
        JdbcPaymentRepository repository = new JdbcPaymentRepository(provider, config.transactionTable());
        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(3),
                TransactionState.PENDING,
                100L,
                200L);
        repository.create(transaction);

        // Act
        int expired = repository.expireOverdue(300L);

        // Assert
        assertEquals(1, expired);
        assertEquals(
                TransactionState.EXPIRED,
                repository
                        .find(transaction.id())
                        .orElseThrow(AssertionError::new)
                        .state());
        provider.close();
    }
}
