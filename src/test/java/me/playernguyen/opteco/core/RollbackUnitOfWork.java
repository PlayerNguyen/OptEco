package me.playernguyen.opteco.core;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.core.model.LedgerEntry;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.port.UnitOfWork;

/** Test double that restores the in-memory repositories when a transaction fails. */
public final class RollbackUnitOfWork implements UnitOfWork {

    private final InMemoryAccountRepository accounts;
    private final InMemoryPaymentRepository payments;
    private final InMemoryLedgerRepository ledger;

    public RollbackUnitOfWork(
            InMemoryAccountRepository accounts, InMemoryPaymentRepository payments, InMemoryLedgerRepository ledger) {
        this.accounts = accounts;
        this.payments = payments;
        this.ledger = ledger;
    }

    @Override
    public <T> T execute(TransactionWork<T> work) {
        Map<UUID, Account> accountSnapshot = accounts.snapshot();
        Map<String, Transaction> paymentSnapshot = payments.snapshot();
        List<LedgerEntry> ledgerSnapshot = ledger.snapshot();
        try {
            return work.execute(null);
        } catch (SQLException exception) {
            restore(accountSnapshot, paymentSnapshot, ledgerSnapshot);
            throw new StorageException("test transaction failed", exception);
        } catch (RuntimeException exception) {
            restore(accountSnapshot, paymentSnapshot, ledgerSnapshot);
            throw exception;
        }
    }

    private void restore(
            Map<UUID, Account> accountSnapshot,
            Map<String, Transaction> paymentSnapshot,
            List<LedgerEntry> ledgerSnapshot) {
        accounts.restore(accountSnapshot);
        payments.restore(paymentSnapshot);
        ledger.restore(ledgerSnapshot);
    }
}
