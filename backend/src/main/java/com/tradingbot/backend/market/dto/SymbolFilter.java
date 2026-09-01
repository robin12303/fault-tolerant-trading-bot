package com.tradingbot.backend.market.dto;

import java.math.BigDecimal;

public record SymbolFilter(
        String filterType,

        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal tickSize,

        BigDecimal minQty,
        BigDecimal maxQty,
        BigDecimal stepSize,

        BigDecimal minNotional,
        BigDecimal maxNotional
) {
}