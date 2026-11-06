package me.playernguyen.opteco.placeholder;

import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One placeholder evaluation request.
 *
 * <p>The request carries everything a {@link PlaceholderStrategy} may need about
 * the call site. Today that is only the player, which is {@code null} when the
 * provider evaluates a player-less context. Wrapping the player in a value
 * object keeps the strategy and registry signatures stable when more request
 * data is added later, instead of forcing every strategy to change.</p>
 */
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlaceholderRequest {

    /** Player the placeholder is evaluated for, or {@code null} when unavailable. */
    @Getter(onMethod_ = @Nullable)
    private final UUID playerId;

    /**
     * Creates a request for a player, which may be unknown to the provider.
     *
     * @param playerId player UUID, or {@code null} when the provider has no player
     * @return a request carrying the player reference
     */
    @NotNull
    public static PlaceholderRequest of(@Nullable UUID playerId) {
        return new PlaceholderRequest(playerId);
    }

    /**
     * Returns the player UUID as an optional value.
     *
     * @return the player UUID, or empty when the provider has no player
     */
    @NotNull
    public Optional<UUID> player() {
        return Optional.ofNullable(playerId);
    }
}
