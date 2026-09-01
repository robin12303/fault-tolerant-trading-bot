package com.tradingbot.backend.trade.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class OrderJournalServiceIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4");

    @Autowired
    OrderJournalService orderJournalService;

    @Autowired
    OrderRepository orderRepository;

    @Test
    void submissionResultIsPersisted() {

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "integration-accepted-1"
                );

        orderJournalService.recordPending(
                request
        );

        OrderEntity pending =
                orderRepository
                        .findByClientOrderId(
                                "integration-accepted-1"
                        )
                        .orElseThrow();

        assertEquals(
                OrderJournalStatus.PENDING_SUBMISSION,
                pending.getStatus()
        );

        OrderSubmissionResult result =
                new OrderSubmissionResult(
                        OrderSubmissionStatus.ACCEPTED,
                        "integration-accepted-1",
                        12345L
                );

        orderJournalService
                .recordSubmissionResult(
                        "integration-accepted-1",
                        result
                );

        OrderEntity updated =
                orderRepository
                        .findByClientOrderId(
                                "integration-accepted-1"
                        )
                        .orElseThrow();

        assertEquals(
                OrderJournalStatus.ACCEPTED,
                updated.getStatus()
        );

        assertEquals(
                12345L,
                updated.getExchangeOrderId()
        );
    }

    @Test
    void recoverableOrdersAreReturned() {

        OrderRequest pendingRequest =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "integration-pending-1"
                );

        OrderRequest unknownRequest =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "integration-unknown-1"
                );

        OrderRequest rejectedRequest =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "integration-rejected-1"
                );

        orderJournalService.recordPending(
                pendingRequest
        );

        orderJournalService.recordPending(
                unknownRequest
        );

        orderJournalService.recordPending(
                rejectedRequest
        );

        orderJournalService.recordSubmissionResult(
                "integration-unknown-1",
                new OrderSubmissionResult(
                        OrderSubmissionStatus.UNKNOWN,
                        "integration-unknown-1",
                        null
                )
        );

        orderJournalService.recordSubmissionResult(
                "integration-rejected-1",
                new OrderSubmissionResult(
                        OrderSubmissionStatus.REJECTED,
                        "integration-rejected-1",
                        null
                )
        );

        List<OrderEntity> recoverable =
                orderJournalService
                        .findRecoverableOrders();

        assertTrue(
                recoverable.stream()
                        .anyMatch(
                                order ->
                                        order.getClientOrderId()
                                                .equals(
                                                        "integration-pending-1"
                                                )
                        )
        );

        assertTrue(
                recoverable.stream()
                        .anyMatch(
                                order ->
                                        order.getClientOrderId()
                                                .equals(
                                                        "integration-unknown-1"
                                                )
                        )
        );

        assertTrue(
                recoverable.stream()
                        .noneMatch(
                                order ->
                                        order.getClientOrderId()
                                                .equals(
                                                        "integration-rejected-1"
                                                )
                        )
        );
    }

    @Test
    void unknownSnapshotDoesNotEraseKnownExchangeOrderId() {

        String clientOrderId =
                "integration-unknown-preserve-1";

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        clientOrderId
                );

        orderJournalService.recordPending(
                request
        );

        orderJournalService.recordSubmissionResult(
                clientOrderId,
                new OrderSubmissionResult(
                        OrderSubmissionStatus.ACCEPTED,
                        clientOrderId,
                        12345L
                )
        );

        orderJournalService.recordExchangeSnapshot(
                clientOrderId,
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        clientOrderId,
                        null,
                        ExchangeOrderStatus.UNKNOWN,
                        BigDecimal.ZERO
                )
        );

        OrderEntity saved =
                orderRepository
                        .findByClientOrderId(
                                clientOrderId
                        )
                        .orElseThrow();

        assertEquals(
                OrderJournalStatus.UNKNOWN,
                saved.getStatus()
        );

        assertEquals(
                12345L,
                saved.getExchangeOrderId()
        );

        assertEquals(
                0,
                saved.getExecutedQty()
                        .compareTo(BigDecimal.ZERO)
        );
    }

    @Test
    void partialFillSnapshotIsPersisted() {

        String clientOrderId =
                "integration-partial-1";

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        clientOrderId
                );

        orderJournalService.recordPending(
                request
        );

        orderJournalService.recordExchangeSnapshot(
                clientOrderId,
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        clientOrderId,
                        98765L,
                        ExchangeOrderStatus.PARTIALLY_FILLED,
                        new BigDecimal("0.003")
                )
        );

        OrderEntity saved =
                orderRepository
                        .findByClientOrderId(
                                clientOrderId
                        )
                        .orElseThrow();

        assertEquals(
                OrderJournalStatus.PARTIALLY_FILLED,
                saved.getStatus()
        );

        assertEquals(
                98765L,
                saved.getExchangeOrderId()
        );

        assertEquals(
                0,
                saved.getExecutedQty()
                        .compareTo(
                                new BigDecimal("0.003")
                        )
        );
    }
}