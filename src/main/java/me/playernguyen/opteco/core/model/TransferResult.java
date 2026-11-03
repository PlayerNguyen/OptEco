package me.playernguyen.opteco.core.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Accessors(fluent = true)
public final class TransferResult {

    @NotNull
    private final TransferStatus status;

    @Nullable
    private final BigDecimal senderBalance;

    @Nullable
    private final BigDecimal receiverBalance;

    private TransferResult(TransferStatus status, BigDecimal senderBalance, BigDecimal receiverBalance) {
        this.status = Objects.requireNonNull(status, "status");
        this.senderBalance = senderBalance == null ? null : Money.normalize(senderBalance);
        this.receiverBalance = receiverBalance == null ? null : Money.normalize(receiverBalance);
    }

    @NotNull
    public static TransferResult success(@NotNull BigDecimal senderBalance, @NotNull BigDecimal receiverBalance) {
        return new TransferResult(TransferStatus.SUCCESS, senderBalance, receiverBalance);
    }

    @NotNull
    public static TransferResult failure(@NotNull TransferStatus status) {
        return new TransferResult(status, null, null);
    }

    @NotNull
    public static TransferResult insufficientFunds(@NotNull BigDecimal senderBalance) {
        return new TransferResult(TransferStatus.INSUFFICIENT_FUNDS, senderBalance, null);
    }

    public boolean isSuccess() {
        return status == TransferStatus.SUCCESS;
    }
}
