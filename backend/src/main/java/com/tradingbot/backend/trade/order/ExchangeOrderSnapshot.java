package com.tradingbot.backend.trade.order;

import java.math.BigDecimal;

public record ExchangeOrderSnapshot(
        String symbol,
        String clientOrderId,
        Long exchangeOrderId,
        ExchangeOrderStatus status,
        BigDecimal executedQty
) {
}