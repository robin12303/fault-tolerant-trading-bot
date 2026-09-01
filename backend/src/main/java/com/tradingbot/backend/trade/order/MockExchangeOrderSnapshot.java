package com.tradingbot.backend.trade.order;

import java.math.BigDecimal;

public record MockExchangeOrderSnapshot(
        MockExchangeOrderStatus status,
        BigDecimal executedQty
) {
}