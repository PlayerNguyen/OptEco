package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of an immediate balance transfer.
 *
 * <p>The status explains whether the transfer happened. Both balances are
 * present after a success, and {@link #insufficientFunds(BigDecimal)} carries
 * the sender's unchanged balance so callers can report how much was available.
 * Instances are immutable and thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
public final class TransferResult {

    /** Outcome category for the transfer. */
    @NotNull
    private final TransferStatus status;

    /** Sender balance after the transfer, or the unchanged balance when funds were insufficient. */
    @Nullable
    private final BigDecimal senderBalance;

    /** Receiver balance after the transfer, or null when no funds moved. */
    @Nullable
    private final BigDecimal receiverBalance;

    /**
     * Creates a result; use the static factories to express the outcome.
     *
     * @param status outcome category
     * @param senderBalance sender balance, or null
     * @param receiverBalance receiver balance, or null
     */
    private TransferResult(TransferStatus status, BigDecimal senderBalance, BigDecimal receiverBalance) {
        this.status = Objects.requireNonNull(status, "status");
        this.senderBalance = senderBalance == null ? null : Money.normalize(senderBalance);
        this.receiverBalance = receiverBalance == null ? null : Money.normalize(receiverBalance);
    }

    /**
     * Creates a successful result with both resulting balances.
     *
     * @param senderBalance sender balance after the transfer
     * @param receiverBalance receiver balance after the transfer
     * @return a result with status {@link TransferStatus#SUCCESS}
     * @throws IllegalArgumentException if either balance has more than {@link Money#SCALE} decimal places
     */
    @NotNull
    public static TransferResult success(@NotNull BigDecimal senderBalance, @NotNull BigDecimal receiverBalance) {
        return new TransferResult(TransferStatus.SUCCESS, senderBalance, receiverBalance);
    }

    /**
     * Creates a failure result that carries no balances.
     *
     * @param status failure category
     * @return a result with null balances
     * @throws NullPointerException if {@code status} is null
     */
    @NotNull
    public static TransferResult failure(@NotNull TransferStatus status) {
        return new TransferResult(status, null, null);
    }

    /**
     * Creates an insufficient-funds result that reports the sender balance.
     *
     * @param senderBalance sender's unchanged balance
     * @return a result with status {@link TransferStatus#INSUFFICIENT_FUNDS}
     * @throws IllegalArgumentException if {@code senderBalance} has more than {@link Money#SCALE} decimal places
     */
    @NotNull
    public static TransferResult insufficientFunds(@NotNull BigDecimal senderBalance) {
        return new TransferResult(TransferStatus.INSUFFICIENT_FUNDS, senderBalance, null);
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
