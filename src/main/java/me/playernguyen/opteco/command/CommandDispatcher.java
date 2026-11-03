package me.playernguyen.opteco.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.playernguyen.opteco.message.MessageKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for the plugin command. It implements Bukkit's
 * {@link CommandExecutor} and {@link TabCompleter}, owns the registry of
 * {@link SubCommand} instances, and performs the checks that every subcommand
 * shares: help handling, permission, player-only enforcement, argument
 * splitting, and error reporting.
 *
 * <p>A dispatcher is created once during startup and registered with the
 * server; {@link #register} is only called from the subclass constructor. All
 * entry points ({@link #onCommand} and {@link #onTabComplete}) are invoked by
 * the server on the main thread and must stay cheap: subcommands hand database
 * work to {@link me.playernguyen.opteco.runtime.AsyncExecutor}.
 */
public abstract class CommandDispatcher implements CommandExecutor, TabCompleter {

    /** Primary label used in help output, without the leading slash. */
    private final String name;

    /** Services shared with every registered subcommand. */
    private final CommandDependencies dependencies;

    /** Registered subcommands keyed by lower-case name, in registration order. */
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    /**
     * Creates a dispatcher and stores its shared services.
     *
     * @param name primary command label used when printing help
     * @param dependencies services shared with the subcommands
     */
    protected CommandDispatcher(@NotNull String name, @NotNull CommandDependencies dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    /**
     * Adds a subcommand to the registry, replacing any entry with the same
     * lower-case name. Intended to be called from the subclass constructor.
     *
     * @param subCommand subcommand instance to expose
     */
    protected final void register(@NotNull SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
    }

    /**
     * Returns the primary command label.
     *
     * @return the label without the leading slash, for example {@code opteco}
     */
    @NotNull
    public String name() {
        return name;
    }

    /**
     * Dispatches one command invocation to its subcommand.
     *
     * <p>Without arguments, or with {@code help}, the generated help listing is
     * shown. Unknown names report {@link MessageKey#COMMAND_NOT_FOUND} followed
     * by the help listing. The sender must hold the subcommand's permission and,
     * for player-only subcommands, must be a player. Runtime failures are
     * forwarded to {@link CommandContext#handleError} so the sender gets a
     * localized message instead of a stack trace.
     *
     * @param sender the sender that issued the command
     * @param command the Bukkit command being executed
     * @param label the alias used to invoke the command
     * @param args the raw arguments, including the subcommand name at index zero
     * @return always {@code true} so Bukkit does not print its own usage line
     */
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }
        String key = args[0].toLowerCase(Locale.ROOT);
        SubCommand subCommand = subCommands.get(key);
        if (subCommand == null) {
            dependencies.messages().send(sender, MessageKey.COMMAND_NOT_FOUND);
            sendHelp(sender);
            return true;
        }
        if (!subCommand.canUse(sender)) {
            dependencies.messages().send(sender, MessageKey.NO_PERMISSION);
            return true;
        }
        if (subCommand.playerOnly() && !(sender instanceof Player)) {
            dependencies.messages().send(sender, MessageKey.FOR_PLAYER);
            return true;
        }
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        CommandContext context = new CommandContext(dependencies, sender, subArgs);
        try {
            subCommand.execute(context);
        } catch (RuntimeException exception) {
            context.handleError(exception);
        }
        return true;
    }

    /**
     * Produces tab completions for the command.
     *
     * <p>Returns nothing when tab completion is disabled in the configuration.
     * For the first argument it offers the names of subcommands the sender may
     * use plus {@code help}; after that it delegates to the selected
     * subcommand's {@link SubCommand#complete}.
     *
     * @param sender the sender requesting completions
     * @param command the Bukkit command being completed
     * @param alias the alias used to invoke the command
     * @param args the raw arguments typed so far
     * @return suggestions for the current argument, empty when nothing applies
     */
    @Override
    @Nullable
    public List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!dependencies.config().current().tabComplete()) {
            return new ArrayList<>();
        }
        if (args.length <= 1) {
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> suggestions = new ArrayList<>();
            for (SubCommand subCommand : subCommands.values()) {
                if (subCommand.canUse(sender)
                        && subCommand.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    suggestions.add(subCommand.name());
                }
            }
            if ("help".startsWith(prefix)) {
                suggestions.add("help");
            }
            return suggestions;
        }
        SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null || !subCommand.canUse(sender)) {
            return new ArrayList<>();
        }
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        return subCommand.complete(new CommandContext(dependencies, sender, subArgs));
    }

    /**
     * Prints the header and, for each visible subcommand, its usage and
     * description. Subcommands the sender cannot use are omitted.
     *
     * @param sender recipient of the help listing
     */
    private void sendHelp(@NotNull CommandSender sender) {
        dependencies.messages().sendPlain(sender, MessageKey.HELP_HEADER);
        for (SubCommand subCommand : subCommands.values()) {
            if (!subCommand.canUse(sender)) {
                continue;
            }
            dependencies
                    .messages()
                    .sendPlain(
                            sender,
                            MessageKey.HELP_FORMAT,
                            "usage",
                            "/" + name + " " + subCommand.usage(),
                            "description",
                            dependencies.messages().colored(subCommand.description()));
        }
    }
}
