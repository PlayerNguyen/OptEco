package me.playernguyen.opteco.placeholder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable, case-insensitive index of {@link PlaceholderStrategy} instances.
 *
 * <p>The constructor indexes every identifier a strategy declares and rejects
 * duplicates, so two strategies can never silently shadow each other. Lookups
 * normalize the requested identifier to lower case with
 * {@link Locale#ROOT}, which means providers may send {@code Points} or
 * {@code POINTS} for the same entry.</p>
 */
public final class PlaceholderRegistry {

    /** Strategies keyed by normalized identifier; unmodifiable after construction. */
    private final Map<String, PlaceholderStrategy> strategies;

    /**
     * Builds a registry from the supplied strategies.
     *
     * @param strategies strategies whose identifiers should be indexed
     * @throws IllegalArgumentException when a strategy is null or two strategies declare the same identifier
     */
    public PlaceholderRegistry(@NotNull Collection<? extends PlaceholderStrategy> strategies) {
        Map<String, PlaceholderStrategy> index = new LinkedHashMap<>();
        int position = 0;
        for (PlaceholderStrategy strategy : strategies) {
            if (strategy == null) {
                throw new IllegalArgumentException("Placeholder strategy at index " + position + " cannot be null");
            }
            for (String identifier : strategy.identifiers()) {
                String key = normalize(identifier);
                if (index.put(key, strategy) != null) {
                    throw new IllegalArgumentException("Duplicate placeholder identifier: " + identifier);
                }
            }
            position++;
        }
        this.strategies = Collections.unmodifiableMap(index);
    }

    /**
     * Resolves a placeholder identifier for a request.
     *
     * @param request placeholder request carrying the player and future call-site data
     * @param identifier identifier without the provider prefix, matched case-insensitively
     * @return the rendered value, or {@code null} when no strategy handles the identifier
     */
    @Nullable
    public String resolve(@NotNull PlaceholderRequest request, @NotNull String identifier) {
        PlaceholderStrategy strategy = strategies.get(normalize(identifier));
        return strategy == null ? null : strategy.resolve(request);
    }

    /**
     * Returns every registered identifier in declaration order.
     *
     * @return an immutable snapshot of the identifiers, never {@code null}
     */
    @NotNull
    public Set<String> identifiers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(strategies.keySet()));
    }

    /**
     * Normalizes an identifier for lookup.
     *
     * @param identifier identifier supplied by a provider
     * @return lower-case identifier
     * @throws NullPointerException when {@code identifier} is {@code null}
     */
    private static String normalize(@NotNull String identifier) {
        return Objects.requireNonNull(identifier, "Placeholder identifier cannot be null")
                .toLowerCase(Locale.ROOT);
    }
}
