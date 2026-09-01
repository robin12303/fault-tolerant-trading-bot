package com.tradingbot.backend.trade.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trading.safety")
public record TradingSafetyProperties(
        boolean dedicatedAccount
) {
}