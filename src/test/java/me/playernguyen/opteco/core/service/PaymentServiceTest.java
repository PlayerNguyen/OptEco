package me.playernguyen.opteco.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.UUID;
import me.playernguyen.opteco.MoneyAssert;
import me.playernguyen.opteco.TestConfigs;
import me.playernguyen.opteco.core.FixedClock;
import me.playernguyen.opteco.core.InMemoryAccountRepository;
import me.playernguyen.opteco.core.InMemoryPaymentRepository;
import me.playernguyen.opteco.core.RecordingEventBus;
import me.playernguyen.opteco.core.RollbackUnitOfWork;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import me.playernguyen.opteco.core.model.PaymentResult;
import me.playernguyen.opteco.core.model.PaymentStatus;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.WriteThroughAccountCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private InMemoryAccountRepository accounts;
    private InMemoryPaymentRepository repository;
    private RecordingEventBus events;
    private EconomyService economy;
    private PaymentService payments;
    private FixedClock clock;
    private UUID alice;
    private UUID bob;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccountRepository();
        repository = new InMemoryPaymentRepository();
        events = new RecordingEventBus();
        clock = new FixedClock(1_000_000L);
        WriteThroughAccountCache cache = new WriteThroughAccountCache();
        economy = new EconomyService(accounts, new RawUnitOfWork(), cache, events, TestConfigs::config);
        payments = new PaymentService(
                repository, new RollbackUnitOfWork(accounts, repository), economy, events, TestConfigs::config, clock);
        alice = UUID.randomUUID();
        bob = UUID.randomUUID();
        economy.ensureAccount(alice, "Alice");
        economy.ensureAccount(bob, "Bob");
        economy.set(bob, "Bob", BigDecimal.ZERO, BalanceChangeReason.ADMIN_SET);
    }

    @Test
    void shouldTransferMoneyWhenPaymentIsConfirmed() {
        // Arrange
        PaymentResult requested = payments.request(alice, "Alice", bob, "Bob", BigDecimal.valueOf(25));

        // Act
        PaymentResult confirmed = payments.confirm(alice);

        // Assert
        assertEquals(PaymentStatus.SUCCESS, requested.status());
        assertNotNull(requested.transaction());
        assertEquals(PaymentStatus.SUCCESS, confirmed.status());
        MoneyAssert.assertMoney("75", economy.getBalance(alice));
        MoneyAssert.assertMoney("25", economy.getBalance(bob));
        assertEquals(TransactionState.CONFIRMED, confirmed.transaction().state());
        assertFalse(payments.isPending(alice));
    }

    @Test
    void shouldNotTransferMoneyWhenPaymentIsCancelled() {
        // Arrange
        payments.request(alice, "Alice", bob, "Bob", BigDecimal.valueOf(25));

        // Act
        PaymentResult cancelled = payments.cancel(alice);

        // Assert
        assertEquals(PaymentStatus.SUCCESS, cancelled.status());
        assertEquals(TransactionState.CANCELLED, cancelled.transaction().state());
        MoneyAssert.assertMoney("100", economy.getBalance(alice));
        MoneyAssert.assertMoney("0", economy.getBalance(bob));
    }

    @Test
    void shouldExpirePaymentWhenTimeoutHasPassed() {
        // Arrange
        payments.request(alice, "Alice", bob, "Bob", BigDecimal.valueOf(25));
        clock.advance(16_000L);

        // Act
        PaymentResult expired = payments.expire(alice);

        // Assert
        assertEquals(PaymentStatus.EXPIRED, expired.status());
        assertEquals(TransactionState.EXPIRED, expired.transaction().state());
        assertFalse(payments.isPending(alice));
    }

    @Test
    void shouldMarkPaymentFailedWhenBalanceIsGoneAtConfirmation() {
        // Arrange
        payments.request(alice, "Alice", bob, "Bob", BigDecimal.valueOf(80));
        economy.take(alice, "Alice", BigDecimal.valueOf(75), BalanceChangeReason.ADMIN_TAKE);

        // Act
        PaymentResult confirmed = payments.confirm(alice);

        // Assert
        assertEquals(PaymentStatus.INSUFFICIENT_FUNDS, confirmed.status());
        assertEquals(TransactionState.FAILED, confirmed.transaction().state());
        MoneyAssert.assertMoney("25", economy.getBalance(alice));
        MoneyAssert.assertMoney("0", economy.getBalance(bob));
        assertFalse(payments.isPending(alice));
    }

    @Test
    void shouldRejectSecondPaymentWhenOneIsPending() {
        // Arrange
        payments.request(alice, "Alice", bob, "Bob", BigDecimal.valueOf(10));

        // Act
        PaymentResult second = payments.request(alice, "Alice", bob, "Bob", BigDecimal.valueOf(20));

        // Assert
        assertEquals(PaymentStatus.ALREADY_PENDING, second.status());
    }

    private static final class RawUnitOfWork implements me.playernguyen.opteco.core.port.UnitOfWork {
        @Override
        public <T> T execute(TransactionWork<T> work) {
            try {
                return work.execute(null);
            } catch (java.sql.SQLException exception) {
                throw new me.playernguyen.opteco.core.exception.StorageException("test transaction failed", exception);
            }
        }
    }
}
