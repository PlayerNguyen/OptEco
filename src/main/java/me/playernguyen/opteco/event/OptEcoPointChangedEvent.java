package me.playernguyen.opteco.event;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import me.playernguyen.opteco.core.model.BalanceChange;
import me.playernguyen.opteco.core.model.BalanceChangeReason;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class OptEcoPointChangedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;
    private final BigDecimal oldBalance;
    private final BigDecimal newBalance;
    private final BalanceChangeReason reason;

    public OptEcoPointChangedEvent(BalanceChange change) {
        this.player = Objects.requireNonNull(change, "change").player();
        this.oldBalance = change.oldBalance();
        this.newBalance = change.newBalance();
        this.reason = change.reason();
    }

    public UUID player() {
        return player;
    }

    public BigDecimal oldBalance() {
        return oldBalance;
    }

    public BigDecimal newBalance() {
        return newBalance;
    }

    public BalanceChangeReason reason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
