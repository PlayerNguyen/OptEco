package me.playernguyen.opteco.command;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for a single subcommand of the {@code /points} command family.
 *
 * <p>Each implementation supplies a name, the permission node that gates it, a
 * description key for the help listing, a usage fragment, and whether it can
 * only be run by players. Instances are immutable and stateless; the
 * dispatcher registers them once and reuses them for every invocation, so
 * per-invocation state belongs in the supplied {@link CommandContext}.
 *
 * <p>{@link #execute} runs on the main server thread. Implementations must not
 * block on the database there; they submit work through
 * {@link CommandDependencies#async} and reply from its callback.
 */
@Getter
@Accessors(fluent = true)
public abstract class SubCommand {

    /** Lower-case name typed after the command label. */
    private final String name;

    /** Permission node a sender must hold to run this subcommand. */
    private final OptEcoPermission permission;

    /** Message key holding the help description. */
    private final MessageKey description;

    /** Usage fragment without the command label, as shown in help output. */
    private final String usage;

    /** Whether the sender must be a player rather than the console. */
    private final boolean playerOnly;

    /**
     * Creates a subcommand descriptor.
     *
     * @param name lower-case subcommand name
     * @param permission permission node that gates the subcommand
     * @param description message key holding the help description
     * @param usage usage fragment without the command label
     * @param playerOnly whether the sender must be a player
     */
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

    /**
     * Runs the subcommand for one invocation.
     *
     * <p>Called on the main server thread after the dispatcher has verified
     * permission, player-only status, and argument availability. Implementations
     * report their own usage errors and route failures through
     * {@link CommandContext#handleError}.
     *
     * @param context invocation state and shared services
     */
    public abstract void execute(@NotNull CommandContext context);

    /**
     * Supplies tab completions for arguments after the subcommand name.
     *
     * <p>The default implementation offers nothing, which suits subcommands
     * with no arguments.
     *
     * @param context invocation state built from the arguments typed so far
     * @return suggestion list, empty when nothing applies
     */
    @NotNull
    public List<String> complete(@NotNull CommandContext context) {
        return Collections.emptyList();
    }

    /**
     * Checks whether the sender holds this subcommand's permission node.
     *
     * @param sender the sender to test
     * @return {@code true} when {@code sender.hasPermission(permission.node())} passes
     */
    public boolean canUse(@NotNull CommandSender sender) {
        return sender.hasPermission(permission.node());
    }
}
