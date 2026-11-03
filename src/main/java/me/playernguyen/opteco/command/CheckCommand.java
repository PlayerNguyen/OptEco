package me.playernguyen.opteco.command;

import java.util.List;
import me.playernguyen.opteco.core.exception.PlayerNotFoundException;
import me.playernguyen.opteco.message.MessageKey;
import me.playernguyen.opteco.permission.OptEcoPermission;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CheckCommand extends SubCommand {

    public CheckCommand() {
        super("check", OptEcoPermission.CHECK, MessageKey.CHECK_DESCRIPTION, "check [player]", false);
    }

    @Override
    public void execute(@NotNull CommandContext context) {
        if (!context.hasArgs(1)) {
            if (!context.isPlayer()) {
                context.reply(MessageKey.USAGE, "usage", "/points " + usage());
                return;
            }
            Player player = context.player();
            checkSelf(context, player);
            return;
        }

        String targetName = context.arg(0);
        if (targetName == null) {
            context.reply(MessageKey.USAGE, "usage", "/points " + usage());
            return;
        }
        ResolvedPlayer target = context.dependencies().players().resolve(targetName);
        context.dependencies()
                .async()
                .run(
                        () -> {
                            if (!target.online()
                                    && !context.dependencies().economy().hasAccount(target.uuid())) {
                                throw new PlayerNotFoundException(target.name());
                            }
                            return context.dependencies().economy().getBalance(target.uuid());
                        },
                        balance -> context.reply(
                                MessageKey.CHECK_ANOTHER,
                                "who",
                                target.name(),
                                "value",
                                context.dependencies().messages().number(balance),
                                "currency",
                                context.dependencies().config().current().currencySymbol()),
                        context::handleError);
    }

    private void checkSelf(CommandContext context, Player player) {
        context.dependencies()
                .async()
                .run(
                        () -> context.dependencies().economy().getBalance(player.getUniqueId()),
                        balance -> context.reply(
                                MessageKey.CHECK_SELF,
                                "value",
                                context.dependencies().messages().number(balance),
                                "currency",
                                context.dependencies().config().current().currencySymbol()),
                        context::handleError);
    }

    @Override
    @NotNull
    public List<String> complete(@NotNull CommandContext context) {
        if (context.args().length == 1) {
            String prefix = context.arg(0);
            return prefix == null
                    ? java.util.Collections.emptyList()
                    : context.dependencies().players().onlineNames(prefix);
        }
        return java.util.Collections.emptyList();
    }
}
