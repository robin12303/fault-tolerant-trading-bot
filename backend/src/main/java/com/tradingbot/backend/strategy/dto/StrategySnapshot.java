package com.tradingbot.backend.strategy.dto;

import java.math.BigDecimal;

public record StrategySnapshot(
        String symbol,
        BigDecimal todayOpen,
        BigDecimal yesterdayHigh,
        BigDecimal yesterdayLow,
        BigDecimal sma5,
        BigDecimal k,
        BigDecimal target,
        BigDecimal btcSma5
) {
}