package me.playernguyen.opteco.core.port;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface AccountCache {

    @NotNull
    Optional<BigDecimal> find(@NotNull UUID uuid);

    void put(@NotNull UUID uuid, @NotNull BigDecimal balance);

    void remove(@NotNull UUID uuid);

    void clear();
}
