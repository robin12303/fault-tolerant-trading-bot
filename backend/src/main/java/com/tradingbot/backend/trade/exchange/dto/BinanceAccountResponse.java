package com.tradingbot.backend.trade.exchange.dto;

import java.util.List;

public record BinanceAccountResponse(
        List<BinanceBalance> balances
) {
}