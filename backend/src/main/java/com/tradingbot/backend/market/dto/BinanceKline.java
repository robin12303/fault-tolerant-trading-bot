package com.tradingbot.backend.market.dto;

import java.math.BigDecimal;

public record BinanceKline(
        long openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long closeTime
) {
}