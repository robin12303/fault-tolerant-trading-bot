package com.tradingbot.backend.trade.order;

import java.math.BigDecimal;

public record OrderRequest(
        String symbol,
        OrderSide side,
        BigDecimal quoteQuantity,
        String clientOrderId
) {
}