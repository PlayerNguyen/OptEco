package me.playernguyen.opteco.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Thin wrapper around Bukkit's player lookups used by the command layer.
 *
 * <p>Resolving an offline name may touch the server's player-data cache, so
 * callers invoke these methods on the main thread before scheduling database
 * work. The class keeps no state and is therefore safe to share between
 * commands.
 */
public final class PlayerResolver {

    /**
     * Looks up an online player by exact, case-sensitive name.
     *
     * @param name exact player name to find
     * @return the online player, or an empty optional when nobody with that name is online
     */
    @NotNull
    public Optional<Player> online(@NotNull String name) {
        return Optional.ofNullable(Bukkit.getPlayerExact(name));
    }

    /**
     * Lists online player names that start with the given prefix, ignoring
     * case, for tab completion.
     *
     * @param prefix case-insensitive prefix to match
     * @return matching player names in Bukkit's iteration order
     */
    @NotNull
    public List<String> onlineNames(@NotNull String prefix) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName()
                    .toLowerCase(java.util.Locale.ROOT)
                    .startsWith(prefix.toLowerCase(java.util.Locale.ROOT))) {
                names.add(player.getName());
            }
        }
        return names;
    }

    /**
     * Resolves a typed name to a UUID, preferring the online player and falling
     * back to Bukkit's offline-player lookup.
     *
     * <p>This performs Bukkit API calls, so it is invoked from the main thread
     * before async database work. Bukkit may not know the name at all; the
     * returned UUID then does not necessarily belong to a real account.
     *
     * @param name player name to resolve
     * @return the resolved UUID, display name, and online state
     */
    @NotNull
    public ResolvedPlayer resolve(@NotNull String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return new ResolvedPlayer(online.getUniqueId(), online.getName(), true);
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        String resolvedName = offline.getName();
        if (resolvedName == null) {
            resolvedName = name;
        }
        return new ResolvedPlayer(offline.getUniqueId(), resolvedName, false);
    }
}
