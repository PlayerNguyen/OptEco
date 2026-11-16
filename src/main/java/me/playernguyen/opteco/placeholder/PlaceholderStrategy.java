package me.playernguyen.opteco.placeholder;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves one or more placeholder identifiers to a display value.
 *
 * <p>A strategy owns a small, independent piece of placeholder behavior such as
 * the cached balance or the configured currency symbol. Implementations must be
 * stateless apart from their injected dependencies and must not perform database
 * I/O, because placeholder providers evaluate them for scoreboards and chat
 * formats on every refresh. Identifier matching, including aliases and case
 * insensitivity, is handled by {@link PlaceholderRegistry}.</p>
 */
public interface PlaceholderStrategy {

    /**
     * Returns every identifier handled by this strategy.
     *
     * <p>The canonical identifier plus any aliases are returned without the
     * provider prefix, for example {@code points} and {@code balance} for the
     * balance strategy.</p>
     *
     * @return non-empty set of identifiers, never {@code null}
     */
    @NotNull
    Set<String> identifiers();

    /**
     * Renders the placeholder value for a request.
     *
     * @param request placeholder request carrying the player and future call-site data
     * @return display text, never {@code null}
     */
    @NotNull
    String resolve(@NotNull PlaceholderRequest request);
}
