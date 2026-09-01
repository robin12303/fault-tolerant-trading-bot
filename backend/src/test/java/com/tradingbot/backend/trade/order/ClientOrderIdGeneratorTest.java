package com.tradingbot.backend.trade.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientOrderIdGeneratorTest {

    @Test
    void generatesDistinctBotOrderIds() {

        ClientOrderIdGenerator generator =
                new ClientOrderIdGenerator();

        String first =
                generator.next();

        String second =
                generator.next();

        assertNotEquals(
                first,
                second
        );

        assertTrue(
                first.startsWith("bot-")
        );

        assertTrue(
                second.startsWith("bot-")
        );

        assertTrue(
                first.matches(
                        "bot-[0-9a-f]{32}"
                )
        );

        assertTrue(
                second.matches(
                        "bot-[0-9a-f]{32}"
                )
        );
    }
}