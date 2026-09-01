package com.tradingbot.backend.trade.exchange.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(
        TradingSafetyPropertiesTest.TestConfig.class
)
@TestPropertySource(properties = {
        "trading.safety.dedicated-account=true"
})
class TradingSafetyPropertiesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(
            TradingSafetyProperties.class
    )
    static class TestConfig {
    }

    @Autowired
    private TradingSafetyProperties properties;

    @Test
    void bindsDedicatedAccountProperty() {

        assertTrue(
                properties.dedicatedAccount()
        );
    }
}