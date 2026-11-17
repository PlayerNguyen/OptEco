package me.playernguyen.opteco.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import me.playernguyen.opteco.MoneyAssert;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.core.FixedClock;
import me.playernguyen.opteco.core.InMemoryAccountRepository;
import me.playernguyen.opteco.core.InMemoryLedgerRepository;
import me.playernguyen.opteco.core.InMemoryPaymentRepository;
import me.playernguyen.opteco.core.RecordingEventBus;
import me.playernguyen.opteco.core.RollbackUnitOfWork;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.LedgerEntry;
import me.playernguyen.opteco.core.model.TransferResult;
import me.playernguyen.opteco.core.model.TransferStatus;
import me.playernguyen.opteco.core.port.WriteThroughAccountCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyServiceTest {

    private InMemoryAccountRepository accounts;
    private InMemoryPaymentRepository payments;
    private InMemoryLedgerRepository ledger;
    private RecordingEventBus events;
    private EconomyService economy;
    private FixedClock clock;
    private UUID alice;
    private UUID bob;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccountRepository();
        payments = new InMemoryPaymentRepository();
        ledger = new InMemoryLedgerRepository();
        events = new RecordingEventBus();
        clock = new FixedClock(1_000_000L);
        economy = new EconomyService(
                accounts,
                ledger,
                new RollbackUnitOfWork(accounts, payments, ledger),
                new WriteThroughAccountCache(),
                events,
                clock,
                TestConfigs::config);
        alice = UUID.randomUUID();
        bob = UUID.randomUUID();
        economy.ensureAccount(alice, "Alice");
        economy.ensureAccount(bob, "Bob");
        economy.set(bob, "Bob", BigDecimal.ZERO, BalanceChangeReason.ADMIN_SET);
        events.balanceChanges().clear();
        ledger.clear();
    }

    @Test
    void shouldAddAndTakeWhenGivenValidAmounts() {
        // Arrange
        BigDecimal add = BigDecimal.valueOf(50);
        BigDecimal take = BigDecimal.valueOf(20);

        // Act
        economy.add(alice, "Alice", add, BalanceChangeReason.ADMIN_ADD);
        boolean taken = economy.take(alice, "Alice", take, BalanceChangeReason.ADMIN_TAKE);

        // Assert
        assertTrue(taken);
        MoneyAssert.assertMoney("130", economy.getBalance(alice));
        MoneyAssert.assertMoney("130", economy.cachedBalance(alice).get());
        assertEquals(2, events.balanceChanges().size());
        assertEquals(2, ledger.entries().size());
        LedgerEntry added = entry(alice, BalanceChangeReason.ADMIN_ADD);
        MoneyAssert.assertMoney("50", added.amount());
        MoneyAssert.assertMoney("100", added.balanceBefore());
        MoneyAssert.assertMoney("150", added.balanceAfter());
        assertNull(added.counterparty());
        assertNull(added.transactionId());
        LedgerEntry takenEntry = entry(alice, BalanceChangeReason.ADMIN_TAKE);
        MoneyAssert.assertMoney("-20", takenEntry.amount());
        MoneyAssert.assertMoney("150", takenEntry.balanceBefore());
        MoneyAssert.assertMoney("130", takenEntry.balanceAfter());
    }

    @Test
    void shouldRejectTakeWhenItWouldGoBelowMinimumBalance() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(111);

        // Act
        boolean taken = economy.take(alice, "Alice", amount, BalanceChangeReason.ADMIN_TAKE);

        // Assert
        assertFalse(taken);
        MoneyAssert.assertMoney("100", economy.getBalance(alice));
        assertTrue(ledger.entries().isEmpty(), "A rejected take must not write a ledger row");
    }

    @Test
    void shouldTransferWhenSenderHasEnoughPoints() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(25);
        economy.set(alice, "Alice", BigDecimal.valueOf(100), BalanceChangeReason.ADMIN_SET);

        // Act
        TransferResult result = economy.transfer(alice, "Alice", bob, "Bob", amount, BalanceChangeReason.API_TRANSFER);

        // Assert
        assertTrue(result.isSuccess());
        MoneyAssert.assertMoney("75", economy.getBalance(alice));
        MoneyAssert.assertMoney("25", economy.getBalance(bob));
        assertEquals(2, events.balanceChanges().size());
        assertEquals(2, ledger.entries().size());
        LedgerEntry sender = entry(alice, BalanceChangeReason.API_TRANSFER);
        MoneyAssert.assertMoney("-25", sender.amount());
        MoneyAssert.assertMoney("100", sender.balanceBefore());
        MoneyAssert.assertMoney("75", sender.balanceAfter());
        assertEquals(bob, sender.counterparty());
        assertNull(sender.transactionId());
        LedgerEntry receiver = entry(bob, BalanceChangeReason.API_TRANSFER);
        MoneyAssert.assertMoney("25", receiver.amount());
        MoneyAssert.assertMoney("0", receiver.balanceBefore());
        MoneyAssert.assertMoney("25", receiver.balanceAfter());
        assertEquals(alice, receiver.counterparty());
    }

    @Test
    void shouldNotMovePointsWhenTransferHasInsufficientFunds() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(500);

        // Act
        TransferResult result = economy.transfer(alice, "Alice", bob, "Bob", amount, BalanceChangeReason.API_TRANSFER);

        // Assert
        assertEquals(TransferStatus.INSUFFICIENT_FUNDS, result.status());
        MoneyAssert.assertMoney("100", economy.getBalance(alice));
        MoneyAssert.assertMoney("0", economy.getBalance(bob));
        assertTrue(ledger.entries().isEmpty(), "A failed transfer must not write ledger rows");
    }

    @Test
    void shouldSetBalanceToZeroWhenRequested() {
        // Act
        economy.set(alice, "Alice", BigDecimal.ZERO, BalanceChangeReason.ADMIN_SET);

        // Assert
        MoneyAssert.assertMoney("0", economy.getBalance(alice));
        assertTrue(economy.hasAccount(alice));
        assertEquals(1, ledger.entries().size());
        LedgerEntry entry = entry(alice, BalanceChangeReason.ADMIN_SET);
        MoneyAssert.assertMoney("-100", entry.amount());
        MoneyAssert.assertMoney("100", entry.balanceBefore());
        MoneyAssert.assertMoney("0", entry.balanceAfter());
    }

    @Test
    void shouldNotWriteLedgerRowWhenSetKeepsTheSameBalance() {
        // Act
        economy.set(alice, "Alice", BigDecimal.valueOf(100), BalanceChangeReason.ADMIN_SET);

        // Assert
        MoneyAssert.assertMoney("100", economy.getBalance(alice));
        assertTrue(ledger.entries().isEmpty(), "Setting the current balance must not write a ledger row");
        assertTrue(events.balanceChanges().isEmpty());
    }

    @Test
    void shouldRecordLedgerTimestampFromWhenTheChangeIsCommitted() {
        // Act
        economy.add(alice, "Alice", BigDecimal.ONE, BalanceChangeReason.API_ADD);

        // Assert
        assertEquals(clock.now(), ledger.entries().get(0).createdAt());
    }

    @Test
    void shouldRollBackBalanceWhenLedgerAppendFails() {
        // Arrange
        ledger.failOnAppend(1);

        // Act & Assert
        assertThrows(
                StorageException.class,
                () -> economy.add(alice, "Alice", BigDecimal.valueOf(25), BalanceChangeReason.API_ADD));
        MoneyAssert.assertMoney("100", economy.getBalance(alice));
        assertTrue(ledger.entries().isEmpty());
        assertTrue(events.balanceChanges().isEmpty());
    }

    @Test
    void shouldRollBackBothTransferLegsWhenSecondLedgerAppendFails() {
        // Arrange
        ledger.failOnAppend(2);

        // Act & Assert
        assertThrows(
                StorageException.class,
                () -> economy.transfer(
                        alice, "Alice", bob, "Bob", BigDecimal.valueOf(25), BalanceChangeReason.API_TRANSFER));
        MoneyAssert.assertMoney("100", economy.getBalance(alice));
        MoneyAssert.assertMoney("0", economy.getBalance(bob));
        assertTrue(ledger.entries().isEmpty());
        assertTrue(events.balanceChanges().isEmpty());
    }

    private LedgerEntry entry(UUID player, BalanceChangeReason reason) {
        return ledger.entries().stream()
                .filter(candidate -> candidate.player().equals(player))
                .filter(candidate -> candidate.reason() == reason)
                .findFirst()
                .orElseThrow(AssertionError::new);
    }
}
