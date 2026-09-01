package com.tradingbot.backend.trade.recovery;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangePositionProvider {

    Map<String, BigDecimal> getPositions();
}