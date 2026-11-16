package me.playernguyen.opteco.placeholder.strategy;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import me.playernguyen.opteco.placeholder.PlaceholderRequest;
import me.playernguyen.opteco.placeholder.PlaceholderStrategy;
import org.jetbrains.annotations.NotNull;

/**
 * Renders the plugin version for the {@code version} identifier.
 *
 * <p>The version is supplied by the plugin rather than read from a static
 * constant, so it stays correct when the server replaces the jar. The value does
 * not depend on the player.</p>
 */
public final class VersionPlaceholderStrategy implements PlaceholderStrategy {

    /** Identifier handled by this strategy. */
    private static final Set<String> IDENTIFIERS = Collections.singleton("version");

    /** Supplies the running plugin version. */
    private final Supplier<String> version;

    /**
     * Creates the strategy.
     *
     * @param version supplies the running plugin version
     */
    public VersionPlaceholderStrategy(@NotNull Supplier<String> version) {
        this.version = version;
    }

    /**
     * Returns the supported identifier.
     *
     * @return always {@code version}
     */
    @Override
    @NotNull
    public Set<String> identifiers() {
        return IDENTIFIERS;
    }

    /**
     * Returns the running plugin version.
     *
     * @param request ignored; the version is global
     * @return the plugin version
     */
    @Override
    @NotNull
    public String resolve(@NotNull PlaceholderRequest request) {
        return version.get();
    }
}
