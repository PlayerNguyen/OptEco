package me.playernguyen.opteco.core.service;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.model.TransferStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Accessors(fluent = true)
public final class TransferOutcome {

    @NotNull
    private final TransferStatus status;

    @NotNull
    private final UUID sender;

    @Nullable
    private final UUID receiver;

    @Nullable
    private final BigDecimal oldSenderBalance;

    @Nullable
    private final BigDecimal newSenderBalance;

    @Nullable
    private final BigDecimal oldReceiverBalance;

    @Nullable
    private final BigDecimal newReceiverBalance;

    private TransferOutcome(
            TransferStatus status,
            UUID sender,
            UUID receiver,
            BigDecimal oldSenderBalance,
            BigDecimal newSenderBalance,
            BigDecimal oldReceiverBalance,
            BigDecimal newReceiverBalance) {
        this.status = status;
        this.sender = sender;
        this.receiver = receiver;
        this.oldSenderBalance = oldSenderBalance;
        this.newSenderBalance = newSenderBalance;
        this.oldReceiverBalance = oldReceiverBalance;
        this.newReceiverBalance = newReceiverBalance;
    }

    @NotNull
    public static TransferOutcome success(
            @NotNull UUID sender,
            @NotNull UUID receiver,
            @NotNull BigDecimal oldSender,
            @NotNull BigDecimal newSender,
            @NotNull BigDecimal oldReceiver,
            @NotNull BigDecimal newReceiver) {
        return new TransferOutcome(
                TransferStatus.SUCCESS, sender, receiver, oldSender, newSender, oldReceiver, newReceiver);
    }

    @NotNull
    public static TransferOutcome insufficientFunds(@NotNull UUID sender, @NotNull BigDecimal senderBalance) {
        return new TransferOutcome(
                TransferStatus.INSUFFICIENT_FUNDS, sender, null, senderBalance, senderBalance, null, null);
    }

    @NotNull
    public static TransferOutcome failure(@NotNull UUID sender, @NotNull TransferStatus status) {
        return new TransferOutcome(status, sender, null, null, null, null, null);
    }

    public boolean isSuccess() {
        return status == TransferStatus.SUCCESS;
    }
}
