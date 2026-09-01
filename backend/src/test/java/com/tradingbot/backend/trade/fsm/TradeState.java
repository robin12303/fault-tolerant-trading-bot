package com.tradingbot.backend.trade.fsm;

public enum TradeState {
    READY,
    BUYING,
    BOUGHT,
    SELLING,
    UNKNOWN,
    HALTED
}