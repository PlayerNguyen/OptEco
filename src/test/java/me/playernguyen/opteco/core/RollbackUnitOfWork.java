package me.playernguyen.opteco.core;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.core.model.Account;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.port.UnitOfWork;

/** Test double that restores the in-memory repositories when a transaction fails. */
public final class RollbackUnitOfWork implements UnitOfWork {

    private final InMemoryAccountRepository accounts;
    private final InMemoryPaymentRepository payments;

    public RollbackUnitOfWork(InMemoryAccountRepository accounts, InMemoryPaymentRepository payments) {
        this.accounts = accounts;
        this.payments = payments;
    }

    @Override
    public <T> T execute(TransactionWork<T> work) {
        Map<UUID, Account> accountSnapshot = accounts.snapshot();
        Map<String, Transaction> paymentSnapshot = payments.snapshot();
        try {
            return work.execute(null);
        } catch (SQLException exception) {
            accounts.restore(accountSnapshot);
            payments.restore(paymentSnapshot);
            throw new StorageException("test transaction failed", exception);
        } catch (RuntimeException exception) {
            accounts.restore(accountSnapshot);
            payments.restore(paymentSnapshot);
            throw exception;
        }
    }
}
