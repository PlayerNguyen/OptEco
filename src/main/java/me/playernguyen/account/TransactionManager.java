package me.playernguyen.account;

import me.playernguyen.OptEco;
import me.playernguyen.schedule.OnTransactionSchedule;

import java.util.ArrayList;
import java.util.UUID;

public class TransactionManager {

    private ArrayList<Transaction> transactions = new ArrayList<>();
    private OptEco plugin;

    public TransactionManager(OptEco plugin) {
        this.plugin = plugin;
    }

    public OptEco getPlugin() {
        return plugin;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public boolean addTransaction(UUID player, UUID target, Double amount) {
        return this.getTransactions()
        .add(new Transaction(getPlugin(), player, target, amount, new OnTransactionSchedule(getPlugin(), player, target)));
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
