package me.playernguyen.opteco.core;

import java.util.ArrayList;
import java.util.List;
import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import me.playernguyen.opteco.core.port.EconomyEventBus;

public final class RecordingEventBus implements EconomyEventBus {

    private final List<BalanceChange> balanceChanges = new ArrayList<>();
    private final List<TransactionState> transactionStates = new ArrayList<>();

    @Override
    public void balanceChanged(BalanceChange change) {
        balanceChanges.add(change);
    }

    @Override
    public void transactionChanged(Transaction transaction, TransactionState previousState) {
        transactionStates.add(transaction.state());
    }

    public List<BalanceChange> balanceChanges() {
        return balanceChanges;
    }

    public List<TransactionState> transactionStates() {
        return transactionStates;
    }
}
