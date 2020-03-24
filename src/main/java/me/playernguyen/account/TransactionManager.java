package me.playernguyen.account;

import me.playernguyen.OptEco;
import me.playernguyen.schedule.OnTransactionSchedule;
import org.bukkit.entity.Player;

import java.util.ArrayList;

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

    public boolean addTransaction(Player player, Player target, Double amount) {
        return this.getTransactions()
                .add(
                        new Transaction(getPlugin(), player, target, amount, new OnTransactionSchedule(getPlugin(), player, target))
                );
    }

    public Transaction getTransaction(Player player) {
        for (Transaction e : this.getTransactions()) {
            if (e.getPlayer().equals(player)) return e;
        }
        return null;
    }

    public boolean hasTransaction (Player player) {
        return this.getTransaction (player) != null;
    }

    public boolean removeTransaction (Player player) {
        return this.getTransactions().remove(getTransaction(player));
    }
}
