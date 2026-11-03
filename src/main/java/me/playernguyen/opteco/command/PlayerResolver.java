package me.playernguyen.opteco.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerResolver {

    @NotNull
    public Optional<Player> online(@NotNull String name) {
        return Optional.ofNullable(Bukkit.getPlayerExact(name));
    }

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
