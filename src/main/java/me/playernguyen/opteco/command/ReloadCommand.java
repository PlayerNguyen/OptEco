package me.playernguyen.opteco.command;

import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /points reload} subcommand: re-reads the configuration and language
 * files without restarting the server.
 *
 * <p>Gated by {@link OptEcoPermission#RELOAD} and available to the console. The
 * work runs synchronously on the main thread because it only swaps immutable
 * snapshots; storage and connection changes cannot be applied to a running
 * plugin, so those are reported back as a restart requirement.
 */
public final class ReloadCommand extends SubCommand {

    /**
     * Creates the subcommand descriptor.
     */
    public ReloadCommand() {
        super("reload", OptEcoPermission.RELOAD, MessageKey.RELOAD_DESCRIPTION, "reload", false);
    }

    /**
     * Reloads config and messages and reports whether a restart is needed.
     *
     * <p>Always replies with {@link MessageKey#RELOAD_DONE}; when
     * {@link me.playernguyen.opteco.OptEco#reloadConfiguration()} detects changed
     * storage settings it also sends {@link MessageKey#RELOAD_RESTART_REQUIRED}.
     *
     * @param context invocation state used to reply to the sender
     */
    @Override
    public void execute(@NotNull CommandContext context) {
        boolean restartRequired = context.dependencies().plugin().reloadConfiguration();
        context.reply(MessageKey.RELOAD_DONE);
        if (restartRequired) {
            context.reply(MessageKey.RELOAD_RESTART_REQUIRED);
        }
    }
}
