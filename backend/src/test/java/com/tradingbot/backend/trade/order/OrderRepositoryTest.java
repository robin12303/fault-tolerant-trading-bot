package com.tradingbot.backend.trade.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class OrderRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4");

    @Autowired
    OrderRepository orderRepository;

    @Test
    void saveAndFindPendingOrder() {

        LocalDateTime now =
                LocalDateTime.now();

        OrderEntity order =
                new OrderEntity(
                        "bot-123",
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        now
                );

        orderRepository.save(order);

        Optional<OrderEntity> found =
                orderRepository.findByClientOrderId(
                        "bot-123"
                );

        assertTrue(found.isPresent());

        OrderEntity saved =
                found.get();

        assertEquals(
                "bot-123",
                saved.getClientOrderId()
        );

        assertEquals(
                "ETHUSDT",
                saved.getSymbol()
        );

        assertEquals(
                OrderSide.BUY,
                saved.getSide()
        );

        assertEquals(
                "MARKET",
                saved.getType()
        );

        assertEquals(
                OrderJournalStatus.PENDING_SUBMISSION,
                saved.getStatus()
        );

        assertEquals(
                0,
                saved.getRequestedQuoteQty()
                        .compareTo(
                                new BigDecimal("20.00")
                        )
        );

        assertNull(
                saved.getRequestedBaseQty()
        );

        assertNull(
                saved.getExchangeOrderId()
        );

        assertEquals(
                0,
                saved.getExecutedQty()
                        .compareTo(BigDecimal.ZERO)
        );

        assertEquals(
                0,
                saved.getExecutedQuoteQty()
                        .compareTo(BigDecimal.ZERO)
        );
    }
}