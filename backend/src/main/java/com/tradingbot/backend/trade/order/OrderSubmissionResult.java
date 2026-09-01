package com.tradingbot.backend.trade.order;

public record OrderSubmissionResult(
        OrderSubmissionStatus status,
        String clientOrderId,
        Long exchangeOrderId
) {
}