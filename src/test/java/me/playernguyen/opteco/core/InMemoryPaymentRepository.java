package me.playernguyen.opteco.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.PaymentRepository;

public final class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Transaction> transactions = new HashMap<>();

    @Override
    public void create(Transaction transaction) {
        transactions.put(transaction.id(), transaction);
    }

    @Override
    public void create(Connection connection, Transaction transaction) throws SQLException {
        create(transaction);
    }

    @Override
    public Optional<Transaction> find(Connection connection, String id) throws SQLException {
        return find(id);
    }

    @Override
    public Optional<Transaction> find(String id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public Optional<Transaction> findPendingBySender(UUID sender) {
        return transactions.values().stream()
                .filter(transaction -> transaction.sender().equals(sender))
                .filter(Transaction::isPending)
                .max(Comparator.comparingLong(Transaction::createdAt));
    }

    @Override
    public Optional<Transaction> findPendingBySender(Connection connection, UUID sender) throws SQLException {
        return findPendingBySender(sender);
    }

    @Override
    public boolean transition(Connection connection, String id, TransactionState expected, TransactionState next)
            throws SQLException {
        return transition(id, expected, next);
    }

    @Override
    public boolean transition(String id, TransactionState expected, TransactionState next) {
        Transaction transaction = transactions.get(id);
        if (transaction == null || transaction.state() != expected) {
            return false;
        }
        transactions.put(id, transaction.withState(next));
        return true;
    }

    @Override
    public int expireOverdue(Connection connection, long now) throws SQLException {
        return expireOverdue(now);
    }

    @Override
    public int expireOverdue(long now) {
        int count = 0;
        for (Transaction transaction : new ArrayList<>(transactions.values())) {
            if (transaction.isPending() && transaction.isExpired(now)) {
                transactions.put(transaction.id(), transaction.withState(TransactionState.EXPIRED));
                count++;
            }
        }
        return count;
    }

    @Override
    public List<Transaction> findRecentBySender(UUID sender, int limit) {
        return transactions.values().stream()
                .filter(transaction -> transaction.sender().equals(sender))
                .sorted(Comparator.comparingLong(Transaction::createdAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Transaction transaction(String id) {
        return transactions.get(id);
    }

    public Map<String, Transaction> snapshot() {
        return new HashMap<>(transactions);
    }

    public void restore(Map<String, Transaction> snapshot) {
        transactions.clear();
        transactions.putAll(snapshot);
    }
}
