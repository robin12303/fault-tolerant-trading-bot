package com.tradingbot.backend.market.dto;

import java.util.List;

public record SymbolInfo(
        String symbol,
        String status,
        String baseAsset,
        String quoteAsset,
        boolean isSpotTradingAllowed,
        boolean quoteOrderQtyMarketAllowed,
        List<SymbolFilter> filters
) {
}