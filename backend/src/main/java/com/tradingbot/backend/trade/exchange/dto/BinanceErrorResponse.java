package com.tradingbot.backend.trade.exchange.dto;

public record BinanceErrorResponse(
        int code,
        String msg
) {
}