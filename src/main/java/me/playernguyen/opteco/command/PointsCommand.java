package me.playernguyen.opteco.command;

import org.jetbrains.annotations.NotNull;

/**
 * Concrete {@code /opteco} dispatcher. The command is declared in
 * {@code plugin.yml} with the aliases {@code /points}, {@code /pts}, and
 * {@code /p}.
 *
 * <p>The constructor registers every subcommand in the order used by the help
 * listing: administration ({@code add}, {@code take}, {@code set}),
 * inspection ({@code check}, {@code me}, {@code top}), payments ({@code pay},
 * {@code confirm}, {@code cancel}), and {@code reload}. Registration happens
 * once during plugin startup; the instance is then read-only apart from the
 * underlying services.
 */
public final class PointsCommand extends CommandDispatcher {

    /**
     * Builds the dispatcher and registers all subcommands.
     *
     * @param dependencies services shared with every subcommand
     */
    public PointsCommand(@NotNull CommandDependencies dependencies) {
        super("opteco", dependencies);
        register(new AddCommand());
        register(new TakeCommand());
        register(new SetCommand());
        register(new CheckCommand());
        register(new MeCommand());
        register(new PayCommand());
        register(new ConfirmCommand());
        register(new CancelCommand());
        register(new TopCommand());
        register(new ReloadCommand());
    }
}
