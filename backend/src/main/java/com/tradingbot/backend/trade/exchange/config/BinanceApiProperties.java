package com.tradingbot.backend.trade.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "binance.api")
public record BinanceApiProperties(
        String key,
        String secret
) {
}