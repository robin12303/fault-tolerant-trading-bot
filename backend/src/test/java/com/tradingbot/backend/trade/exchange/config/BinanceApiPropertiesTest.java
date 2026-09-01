package com.tradingbot.backend.trade.exchange.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(BinanceApiPropertiesTest.TestConfig.class)
@TestPropertySource(properties = {
        "binance.api.key=test-key",
        "binance.api.secret=test-secret"
})
class BinanceApiPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(BinanceApiProperties.class)
    static class TestConfig {
    }

    @Autowired
    private BinanceApiProperties properties;

    @Test
    void bindsBinanceApiProperties() {

        assertEquals(
                "test-key",
                properties.key()
        );

        assertEquals(
                "test-secret",
                properties.secret()
        );
    }
}