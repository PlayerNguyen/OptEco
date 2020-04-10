package me.playernguyen.opteco.transaction;

import java.util.UUID;

public class TransactionResult {

    private String id;
    private UUID sender;
    private UUID receiver;
    private double amount;
    private TransactionState state;

    public TransactionResult(String id, UUID sender, UUID receiver, double amount, TransactionState state) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.state = state;
    }

}
