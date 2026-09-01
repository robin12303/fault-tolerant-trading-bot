package com.tradingbot.backend.trade.order;

public interface OrderQueryClient {

    ExchangeOrderSnapshot getOrder(
            String symbol,
            String clientOrderId
    );
}