package com.tradingbot.backend.trade.recovery;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class UnavailableExchangePositionProvider
        implements ExchangePositionProvider {

    @Override
    public Map<String, BigDecimal> getPositions() {
        throw new IllegalStateException(
                "Exchange position provider is not implemented yet"
        );
    }
}