package com.tradingbot.backend.trade.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trading.execution")
public record TradingExecutionProperties(
        boolean enabled
) {
}