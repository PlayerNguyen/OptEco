package me.playernguyen.opteco.command;

import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
@Accessors(fluent = true)
public final class ResolvedPlayer {

    private final UUID uuid;
    private final String name;
    private final boolean online;

    public ResolvedPlayer(@NotNull UUID uuid, @NotNull String name, boolean online) {
        this.uuid = uuid;
        this.name = name;
        this.online = online;
    }
}
