package me.playernguyen.opteco.core.service;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.playernguyen.opteco.core.model.TransferStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a transfer performed inside a database transaction.
 *
 * <p>Unlike {@link me.playernguyen.opteco.core.model.TransferResult}, this
 * carries the old and new balances for both players so
 * {@link EconomyService#completeTransfer} can update the cache and publish one
 * balance-change event per participant after the transaction commits.
 * Instances are immutable and thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
public final class TransferOutcome {

    /** Outcome category for the transfer. */
    @NotNull
    private final TransferStatus status;

    /** Player funding the transfer. */
    @NotNull
    private final UUID sender;

    /** Player receiving the funds, or null when the failure happened before the receiver was known. */
    @Nullable
    private final UUID receiver;

    /** Sender balance before the transfer, or the unchanged balance on insufficient funds. */
    @Nullable
    private final BigDecimal oldSenderBalance;

    /** Sender balance after the transfer, or the unchanged balance on insufficient funds. */
    @Nullable
    private final BigDecimal newSenderBalance;

    /** Receiver balance before the transfer, or null unless the transfer succeeded. */
    @Nullable
    private final BigDecimal oldReceiverBalance;

    /** Receiver balance after the transfer, or null unless the transfer succeeded. */
    @Nullable
    private final BigDecimal newReceiverBalance;

    /**
     * Creates a transfer outcome; use the static factories instead of calling this directly.
     *
     * @param status outcome category
     * @param sender player funding the transfer
     * @param receiver player receiving the funds, or null
     * @param oldSenderBalance sender balance before the transfer, or null
     * @param newSenderBalance sender balance after the transfer, or null
     * @param oldReceiverBalance receiver balance before the transfer, or null
     * @param newReceiverBalance receiver balance after the transfer, or null
     */
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

    /**
     * Creates a successful outcome with both old and new balances.
     *
     * @param sender player funding the transfer
     * @param receiver player receiving the funds
     * @param oldSender sender balance before the transfer
     * @param newSender sender balance after the transfer
     * @param oldReceiver receiver balance before the transfer
     * @param newReceiver receiver balance after the transfer
     * @return a successful outcome
     */
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

    /**
     * Creates an insufficient-funds outcome that reports the unchanged sender balance.
     *
     * @param sender player who attempted the transfer
     * @param senderBalance sender's unchanged balance
     * @return an outcome with status {@link TransferStatus#INSUFFICIENT_FUNDS}
     */
    @NotNull
    public static TransferOutcome insufficientFunds(@NotNull UUID sender, @NotNull BigDecimal senderBalance) {
        return new TransferOutcome(
                TransferStatus.INSUFFICIENT_FUNDS, sender, null, senderBalance, senderBalance, null, null);
    }

    /**
     * Creates a failed outcome without balances.
     *
     * @param sender player who attempted the transfer
     * @param status failure category
     * @return an outcome with the supplied failure status
     */
    @NotNull
    public static TransferOutcome failure(@NotNull UUID sender, @NotNull TransferStatus status) {
        return new TransferOutcome(status, sender, null, null, null, null, null);
    }

    /**
     * Tests whether the transfer completed.
     *
     * @return {@code true} when the status is {@link TransferStatus#SUCCESS}
     */
    public boolean isSuccess() {
        return status == TransferStatus.SUCCESS;
    }
}
