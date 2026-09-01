package com.tradingbot.backend.market.dto;

import java.util.List;

public record ExchangeInfoResponse(
        List<SymbolInfo> symbols
) {
}