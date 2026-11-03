package me.playernguyen.opteco.command;

import org.jetbrains.annotations.NotNull;

public final class PointsCommand extends CommandDispatcher {

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
