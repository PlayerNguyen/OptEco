package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Accessors(fluent = true)
public final class PaymentResult {

    @NotNull
    private final PaymentStatus status;

    @Nullable
    private final Transaction transaction;

    @Nullable
    private final BigDecimal senderBalance;

    @Nullable
    private final BigDecimal receiverBalance;

    public PaymentResult(
            PaymentStatus status, Transaction transaction, BigDecimal senderBalance, BigDecimal receiverBalance) {
        this.status = Objects.requireNonNull(status, "status");
        this.transaction = transaction;
        this.senderBalance = senderBalance;
        this.receiverBalance = receiverBalance;
    }

    @NotNull
    public static PaymentResult of(@NotNull PaymentStatus status, @Nullable Transaction transaction) {
        return new PaymentResult(status, transaction, null, null);
    }

    @NotNull
    public static PaymentResult success(
            @NotNull Transaction transaction, @NotNull BigDecimal senderBalance, @NotNull BigDecimal receiverBalance) {
        return new PaymentResult(PaymentStatus.SUCCESS, transaction, senderBalance, receiverBalance);
    }

    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }
}
