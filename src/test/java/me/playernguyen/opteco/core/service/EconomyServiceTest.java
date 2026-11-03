package me.playernguyen.opteco.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import me.playernguyen.opteco.MoneyAssert;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.core.InMemoryAccountRepository;
import me.playernguyen.opteco.core.RecordingEventBus;
import me.playernguyen.opteco.core.TestUnitOfWork;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.TransferResult;
import me.playernguyen.opteco.core.model.TransferStatus;
import me.playernguyen.opteco.core.port.WriteThroughAccountCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EconomyServiceTest {

    private InMemoryAccountRepository accounts;
    private RecordingEventBus events;
    private EconomyService economy;
    private UUID alice;
    private UUID bob;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccountRepository();
        events = new RecordingEventBus();
        economy = new EconomyService(
                accounts, new TestUnitOfWork(), new WriteThroughAccountCache(), events, TestConfigs::config);
        alice = UUID.randomUUID();
        bob = UUID.randomUUID();
        economy.ensureAccount(alice, "Alice");
        economy.ensureAccount(bob, "Bob");
        economy.set(bob, "Bob", BigDecimal.ZERO, BalanceChangeReason.ADMIN_SET);
        events.balanceChanges().clear();
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
    }

    @Test
    void shouldSetBalanceToZeroWhenRequested() {
        // Act
        economy.set(alice, "Alice", BigDecimal.ZERO, BalanceChangeReason.ADMIN_SET);

        // Assert
        MoneyAssert.assertMoney("0", economy.getBalance(alice));
        assertTrue(economy.hasAccount(alice));
    }
}
