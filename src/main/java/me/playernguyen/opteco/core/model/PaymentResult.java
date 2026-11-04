package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of a payment request, confirmation, cancellation, or expiry.
 *
 * <p>The status explains what happened. The transaction is present whenever a
 * payment row was identified, including failed and already-handled attempts;
 * the two balances are populated only by a successful confirmation. The class
 * is immutable and thread-safe.</p>
 */
@Getter
@Accessors(fluent = true)
public final class PaymentResult {

    /** Outcome category for the payment operation. */
    @NotNull
    private final PaymentStatus status;

    /** Payment involved in the operation, or null when none was identified. */
    @Nullable
    private final Transaction transaction;

    /** Sender balance after a successful confirmation, otherwise null. */
    @Nullable
    private final BigDecimal senderBalance;

    /** Receiver balance after a successful confirmation, otherwise null. */
    @Nullable
    private final BigDecimal receiverBalance;

    /**
     * Creates a payment result.
     *
     * @param status outcome category
     * @param transaction payment involved in the operation, or null
     * @param senderBalance sender balance after a successful confirmation, or null
     * @param receiverBalance receiver balance after a successful confirmation, or null
     * @throws NullPointerException if {@code status} is null
     */
    public PaymentResult(
            PaymentStatus status, Transaction transaction, BigDecimal senderBalance, BigDecimal receiverBalance) {
        this.status = Objects.requireNonNull(status, "status");
        this.transaction = transaction;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
    }

    /**
     * Creates a result that carries no balances.
     *
     * @param status outcome category
     * @param transaction payment involved in the operation, or null
     * @return a result with null sender and receiver balances
     * @throws NullPointerException if {@code status} is null
     */
    @NotNull
    public static PaymentResult of(@NotNull PaymentStatus status, @Nullable Transaction transaction) {
        return new PaymentResult(status, transaction, null, null);
    }

    /**
     * Creates a successful confirmation result with both resulting balances.
     *
     * @param transaction confirmed payment
     * @param senderBalance sender balance after the transfer
     * @param receiverBalance receiver balance after the transfer
     * @return a successful result
     */
    @NotNull
    public static PaymentResult success(
            @NotNull Transaction transaction, @NotNull BigDecimal senderBalance, @NotNull BigDecimal receiverBalance) {
        return new PaymentResult(PaymentStatus.SUCCESS, transaction, senderBalance, receiverBalance);
    }

    /**
     * Tests whether the operation completed successfully.
     *
     * @return {@code true} when the status is {@link PaymentStatus#SUCCESS}
     */
    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }
}
