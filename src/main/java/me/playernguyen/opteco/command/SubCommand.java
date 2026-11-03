package me.playernguyen.opteco.command;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@Getter
@Accessors(fluent = true)
public abstract class SubCommand {

    private final String name;
    private final OptEcoPermission permission;
    private final MessageKey description;
    private final String usage;
    private final boolean playerOnly;

    protected SubCommand(
            @NotNull String name,
            @NotNull OptEcoPermission permission,
            @NotNull MessageKey description,
            @NotNull String usage,
            boolean playerOnly) {
        this.name = name;
        this.permission = permission;
        this.description = description;
        this.usage = usage;
        this.playerOnly = playerOnly;
    }

    public abstract void execute(@NotNull CommandContext context);

    @NotNull
    public List<String> complete(@NotNull CommandContext context) {
        return Collections.emptyList();
    }

    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission(permission.node());
    }
}
