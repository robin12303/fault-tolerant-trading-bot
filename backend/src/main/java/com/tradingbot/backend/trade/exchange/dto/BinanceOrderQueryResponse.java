package com.tradingbot.backend.trade.exchange.dto;

import java.math.BigDecimal;

public record BinanceOrderQueryResponse(
        String symbol,
        Long orderId,
        String clientOrderId,
        String status,
        BigDecimal executedQty
) {
}