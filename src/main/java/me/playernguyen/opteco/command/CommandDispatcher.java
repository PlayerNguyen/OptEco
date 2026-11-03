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

public abstract class CommandDispatcher implements CommandExecutor, TabCompleter {

    private final String name;
    private final CommandDependencies dependencies;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    protected CommandDispatcher(@NotNull String name, @NotNull CommandDependencies dependencies) {
        this.name = name;
        this.dependencies = dependencies;
    }

    protected final void register(@NotNull SubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(Locale.ROOT), subCommand);
    }

    @NotNull
    public String name() {
        return name;
    }

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
