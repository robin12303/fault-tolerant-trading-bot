package com.tradingbot.backend.trade.service;

public enum BuyExecutionResult {

    EXECUTION_DISABLED,
    FSM_NOT_READY,

    ACCEPTED,
    REJECTED,
    RATE_LIMITED,
    UNKNOWN
}