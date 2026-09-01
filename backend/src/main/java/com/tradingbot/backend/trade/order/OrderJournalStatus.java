package com.tradingbot.backend.trade.order;

public enum OrderJournalStatus {

    PENDING_SUBMISSION,

    ACCEPTED,
    RATE_LIMITED,
    UNKNOWN,

    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    REJECTED,
    EXPIRED,
    EXPIRED_IN_MATCH,

    NOT_FOUND
}