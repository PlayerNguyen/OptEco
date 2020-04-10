package me.playernguyen.opteco.event;

import me.playernguyen.opteco.transaction.Transaction;

public class OptEcoPlayerReceivedEvent extends TransactionEvent {

    public OptEcoPlayerReceivedEvent(Transaction transaction) {
        super(transaction);
    }

}
