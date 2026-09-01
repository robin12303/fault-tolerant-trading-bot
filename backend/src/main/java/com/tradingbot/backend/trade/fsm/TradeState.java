package com.tradingbot.backend.trade.fsm;

public enum TradeState {
    READY,
    BUYING,
    BUY_SUBMITTED,
    BOUGHT,
    SELLING,
    UNKNOWN,
    HALTED
}