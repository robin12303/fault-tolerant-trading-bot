package com.tradingbot.backend.trade.order;

public enum ExchangeOrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    REJECTED,
    EXPIRED,
    EXPIRED_IN_MATCH,
    NOT_FOUND,
    UNKNOWN
}