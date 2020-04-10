package me.playernguyen.opteco.event;

import me.playernguyen.opteco.transaction.Transaction;
import me.playernguyen.opteco.transaction.TransactionState;

public class OptEcoTransactionChangeStateEvent extends TransactionEvent {

    /**
     * Call while the state change
     * @param transaction The transaction
     */
    public OptEcoTransactionChangeStateEvent(Transaction transaction) {
        super(transaction);
    }

    /**
     * Get a state of that transaction
     * @return The {@link TransactionState}
     */
    public TransactionState getState() {
        return getTransaction().getState();
    }

}
