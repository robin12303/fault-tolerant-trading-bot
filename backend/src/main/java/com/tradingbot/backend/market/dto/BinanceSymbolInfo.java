package com.tradingbot.backend.market.dto;

public record BinanceSymbolInfo(
        String symbol,
        String baseAsset,
        String quoteAsset
) {
}