package com.tradingbot.backend.trade.exchange.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TradingOrderPropertiesTest {

    @Test
    void storesBuyQuoteQuantity() {

        TradingOrderProperties properties =
                new TradingOrderProperties(
                        new BigDecimal("20.00")
                );

        assertEquals(
                0,
                properties.buyQuoteQuantity()
                        .compareTo(
                                new BigDecimal("20.00")
                        )
        );
    }
}