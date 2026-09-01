package com.tradingbot.backend.market.dto;

import java.util.List;

public record BinanceExchangeInfoResponse(
        List<BinanceSymbolInfo> symbols
) {
}