package com.tradingbot.backend.trade.exchange.dto;

import java.math.BigDecimal;

public record BinanceBalance(
        String asset,
        BigDecimal free,
        BigDecimal locked
) {
}