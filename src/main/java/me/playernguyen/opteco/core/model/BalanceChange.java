package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BalanceChange {

    private final UUID player;
    private final BigDecimal oldBalance;
    private final BigDecimal newBalance;
    private final BalanceChangeReason reason;

    public BalanceChange(UUID player, BigDecimal oldBalance, BigDecimal newBalance, BalanceChangeReason reason) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldBalance = Money.normalize(oldBalance);
        this.newBalance = Money.normalize(newBalance);
        this.reason = Objects.requireNonNull(reason, "reason");
    }
}
