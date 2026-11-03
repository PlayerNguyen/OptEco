package me.playernguyen.opteco.core.port;

import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.Transaction;
import me.playernguyen.opteco.core.model.TransactionState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EconomyEventBus {

    void balanceChanged(@NotNull BalanceChange change);

    void transactionChanged(@NotNull Transaction transaction, @Nullable TransactionState previousState);
}
