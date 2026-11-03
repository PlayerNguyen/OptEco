package me.playernguyen.opteco.command;

import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.jetbrains.annotations.NotNull;

public final class ReloadCommand extends SubCommand {

    public ReloadCommand() {
        super("reload", OptEcoPermission.RELOAD, MessageKey.RELOAD_DESCRIPTION, "reload", false);
    }

    @Override
    public void execute(@NotNull CommandContext context) {
        boolean restartRequired = context.dependencies().plugin().reloadConfiguration();
        context.reply(MessageKey.RELOAD_DONE);
        if (restartRequired) {
            context.reply(MessageKey.RELOAD_RESTART_REQUIRED);
        }
    }
}
