package me.playernguyen.opteco.core.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import me.playernguyen.opteco.core.model.Money;

public final class WriteThroughAccountCache implements AccountCache {

    private final ConcurrentMap<UUID, BigDecimal> balances = new ConcurrentHashMap<>();

    @Override
    public Optional<BigDecimal> find(UUID uuid) {
        return Optional.ofNullable(balances.get(uuid));
    }

    @Override
    public void put(UUID uuid, BigDecimal balance) {
        balances.put(uuid, Money.normalize(balance));
    }

    @Override
    public void remove(UUID uuid) {
        balances.remove(uuid);
    }

    @Override
    public void clear() {
        balances.clear();
    }
}
