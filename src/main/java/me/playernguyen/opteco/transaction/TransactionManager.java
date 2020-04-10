package me.playernguyen.opteco.transaction;

import me.playernguyen.opteco.OptEcoImplementation;
import me.playernguyen.opteco.configuration.StorageType;
import me.playernguyen.opteco.schedule.TransactionCountRunnable;
import me.playernguyen.opteco.transaction.mysql.TransactionMySQLStorage;

import java.util.ArrayList;
import java.util.UUID;

public class TransactionManager extends OptEcoImplementation {

    private ArrayList<Transaction> transactions = new ArrayList<>();
    private TransactionStorage transactionStorage;
    public TransactionManager() {
        // Setup database
        if (getPlugin().getStorageType().equals(StorageType.MYSQL)) {
            this.transactionStorage = new TransactionMySQLStorage();
        }

    }

    public TransactionStorage getTransactionStorage() {
        return transactionStorage;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(UUID player, UUID target, Double amount) {
        Transaction transaction =
                new Transaction(player, target, amount, new TransactionCountRunnable(player, target));
        this.getTransactions().add(transaction);
        transactionStorage.push(transaction);
    }

    public Transaction getTransaction(UUID player) {
        for (Transaction e : this.getTransactions()) {
            if (e.getPlayer().equals(player)) return e;
        }
        return null;
    }

    public boolean hasTransaction (UUID player) {
        return this.getTransaction (player) != null;
    }

    public boolean removeTransaction (UUID player) {
        return this.getTransactions().remove(getTransaction(player));
    }
}
