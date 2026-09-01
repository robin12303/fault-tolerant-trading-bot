package com.tradingbot.backend.trade.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "trading")
public record TradingSymbolsProperties(
        List<String> symbols
) {
}