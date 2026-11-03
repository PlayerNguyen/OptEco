package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(of = "uuid")
@ToString
public final class Account {

    private final UUID uuid;
    private final String name;
    private final BigDecimal balance;

    public Account(UUID uuid, String name, BigDecimal balance) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = name == null ? "" : name;
        this.balance = Money.normalize(Objects.requireNonNull(balance, "balance"));
    }

    public Account withBalance(BigDecimal newBalance) {
        return new Account(uuid, name, newBalance);
    }

    public Account withName(String newName) {
        return new Account(uuid, newName, balance);
    }
}
