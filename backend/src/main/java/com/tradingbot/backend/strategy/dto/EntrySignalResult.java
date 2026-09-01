package com.tradingbot.backend.strategy.dto;

import java.math.BigDecimal;

public record EntrySignalResult(
        String symbol,
        BigDecimal currentPrice,
        BigDecimal target,
        BigDecimal sma5,
        BigDecimal btcCurrentPrice,
        BigDecimal btcSma5,
        boolean entrySignal
) {
}