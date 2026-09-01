package com.tradingbot.backend.trade.exchange.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "trading.order")
public record TradingOrderProperties(

        @NotNull
        @DecimalMin(
                value = "0.0",
                inclusive = false
        )
        BigDecimal buyQuoteQuantity

) {
}