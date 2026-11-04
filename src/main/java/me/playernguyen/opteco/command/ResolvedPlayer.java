package me.playernguyen.opteco.command;

import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable result of resolving a typed player name: the UUID to use for
 * account lookups, the name to show in messages, and whether the player was
 * online at resolution time.
 *
 * <p>Produced by {@link PlayerResolver} and carried into async tasks, so it
 * deliberately holds no Bukkit player reference.
 */
@Getter
@Accessors(fluent = true)
public final class ResolvedPlayer {

    /** Account identifier for the resolved player. */
    private final UUID uuid;

    /** Display name to use in messages. */
    private final String name;

    /** Whether the player was online when the name was resolved. */
    private final boolean online;

    /**
     * Creates a resolved-player value.
     *
     * @param uuid account identifier
     * @param name display name for messages
     * @param online whether the player was online at resolution time
     */
    public ResolvedPlayer(@NotNull UUID uuid, @NotNull String name, boolean online) {
        this.uuid = uuid;
        this.name = name;
        this.online = online;
    }
}
