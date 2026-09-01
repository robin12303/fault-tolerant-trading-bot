package com.tradingbot.backend.trade.exchange.dto;
public record BinanceNewOrderResponse(
        Long orderId,
        String clientOrderId
) {
}