package me.playernguyen.opteco;

import java.math.BigDecimal;
import me.playernguyen.opteco.config.OptEcoConfig;

public final class TestConfigs {

    private TestConfigs() {}

    public static OptEcoConfig config() {
        OptEcoConfig config = OptEcoConfig.builder()
                .useDouble(true)
                .startBalance(BigDecimal.valueOf(100))
                .minBalance(BigDecimal.valueOf(-10))
                .paymentDurationSeconds(15)
                .build();
        config.validate();
        return config;
    }
}
