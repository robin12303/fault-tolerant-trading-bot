package com.tradingbot.backend.trade.order;

public enum OrderJournalStatus {
    PENDING_SUBMISSION,
    ACCEPTED,
    REJECTED,
    RATE_LIMITED,
    UNKNOWN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED
}