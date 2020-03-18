package me.playernguyen.account;

import org.bukkit.entity.Player;

public class TransactionResponse {

    private Player sender;
    private Player receiver;
    private double deposit;

    public TransactionResponse (Player sender, Player receiver, double deposit) {
        this.receiver = receiver;
        this.sender = sender;
        this.deposit = deposit;
    }

    public Player getReceiver() {
        return receiver;
    }

    public Player getSender() {
        return sender;
    }

    public double getDeposit() {
        return deposit;
    }
}
