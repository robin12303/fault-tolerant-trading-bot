package com.tradingbot.backend.trade.position;

import java.math.BigDecimal;

public record PositionSnapshot(
        String symbol,
        BigDecimal baseQty
) {
}