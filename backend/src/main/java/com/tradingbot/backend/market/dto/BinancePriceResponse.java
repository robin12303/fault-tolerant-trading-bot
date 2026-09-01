package com.tradingbot.backend.market.dto;

import java.math.BigDecimal;

public record BinancePriceResponse(
        String symbol,
        BigDecimal price
) {
}