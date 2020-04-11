package me.playernguyen.opteco.transaction;

import java.util.UUID;

public class TransactionResult {

    private String id;
    private UUID sender;
    private UUID receiver;
    private double amount;
    private TransactionState state;
    private long time;

    public TransactionResult(String id, UUID sender, UUID receiver, double amount, TransactionState state, long time) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.state = state;
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public double getAmount() {
        return amount;
    }

    public TransactionState getState() {
        return state;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "TransactionResult{" +
                "id='" + id + '\'' +
                ", sender=" + sender +
                ", receiver=" + receiver +
                ", amount=" + amount +
                ", state=" + state +
                ", time=" + time +
                '}';
    }
}
