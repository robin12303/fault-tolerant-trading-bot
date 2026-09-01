package com.tradingbot.backend.trade.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class OrderJournalServiceTest {

    @Test
    void recordsPendingOrderBeforeSubmission() {

        OrderRepository orderRepository =
                mock(OrderRepository.class);

        Clock clock =
                Clock.fixed(
                        Instant.parse(
                                "2026-09-01T12:00:00Z"
                        ),
                        ZoneOffset.UTC
                );

        OrderJournalService service =
                new OrderJournalService(
                        orderRepository,
                        clock
                );

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-123"
                );

        when(orderRepository.saveAndFlush(any()))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        OrderEntity saved =
                service.recordPending(request);

        assertSame(
                OrderJournalStatus.PENDING_SUBMISSION,
                saved.getStatus()
        );

        verify(orderRepository)
                .saveAndFlush(any(OrderEntity.class));
    }
}