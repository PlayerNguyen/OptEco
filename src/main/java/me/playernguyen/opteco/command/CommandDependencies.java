package me.playernguyen.opteco.command;

import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.OptEco;
import me.playernguyen.opteco.config.ConfigService;
import me.playernguyen.opteco.core.service.EconomyService;
import me.playernguyen.opteco.core.service.PaymentService;
import me.playernguyen.opteco.message.MessageService;
import me.playernguyen.opteco.runtime.AsyncExecutor;
import org.jetbrains.annotations.NotNull;

@Getter
@Accessors(fluent = true)
public final class CommandDependencies {

    private final OptEco plugin;
    private final ConfigService config;
    private final MessageService messages;
    private final AsyncExecutor async;
    private final EconomyService economy;
    private final PaymentService payments;
    private final PlayerResolver players;

    public CommandDependencies(
            @NotNull OptEco plugin,
            @NotNull ConfigService config,
            @NotNull MessageService messages,
            @NotNull AsyncExecutor async,
            @NotNull EconomyService economy,
            @NotNull PaymentService payments,
            @NotNull PlayerResolver players) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.async = async;
        this.economy = economy;
        this.payments = payments;
        this.players = players;
    }
}
