package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.order.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrderRecoveryServiceTest {

    private OrderEntity order(
            String clientOrderId
    ) {

        return new OrderEntity(
                clientOrderId,
                "ETHUSDT",
                OrderSide.BUY,
                new BigDecimal("20.00"),
                LocalDateTime.now()
        );
    }

    @Test
    void noRecoverableOrdersIsConsistent() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        when(journalService.findRecoverableOrders())
                .thenReturn(List.of());

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        assertEquals(
                StartupReconciliationResult.CONSISTENT,
                service.recover()
        );

        verifyNoInteractions(queryClient);
    }

    @Test
    void filledOrderBecomesConsistent() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderEntity order =
                order("recovery-filled-1");

        when(journalService.findRecoverableOrders())
                .thenReturn(
                        List.of(order)
                );

        ExchangeOrderSnapshot snapshot =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "recovery-filled-1",
                        12345L,
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.01")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-filled-1"
        ))
                .thenReturn(snapshot);

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        assertEquals(
                StartupReconciliationResult.CONSISTENT,
                service.recover()
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "recovery-filled-1",
                        snapshot
                );
    }

    @Test
    void openOrderIsInconsistent() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderEntity order =
                order("recovery-new-1");

        when(journalService.findRecoverableOrders())
                .thenReturn(
                        List.of(order)
                );

        ExchangeOrderSnapshot snapshot =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "recovery-new-1",
                        12345L,
                        ExchangeOrderStatus.NEW,
                        BigDecimal.ZERO
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-new-1"
        ))
                .thenReturn(snapshot);

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        assertEquals(
                StartupReconciliationResult.INCONSISTENT,
                service.recover()
        );
    }

    @Test
    void unknownOrderStatusIsUnavailable() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderEntity order =
                order("recovery-unknown-1");

        when(journalService.findRecoverableOrders())
                .thenReturn(
                        List.of(order)
                );

        ExchangeOrderSnapshot snapshot =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "recovery-unknown-1",
                        null,
                        ExchangeOrderStatus.UNKNOWN,
                        BigDecimal.ZERO
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-unknown-1"
        ))
                .thenReturn(snapshot);

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        assertEquals(
                StartupReconciliationResult.UNAVAILABLE,
                service.recover()
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "recovery-unknown-1",
                        snapshot
                );
    }

    @Test
    void notFoundOrderIsInconsistent() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderEntity order =
                order("recovery-not-found-1");

        when(journalService.findRecoverableOrders())
                .thenReturn(
                        List.of(order)
                );

        ExchangeOrderSnapshot snapshot =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "recovery-not-found-1",
                        null,
                        ExchangeOrderStatus.NOT_FOUND,
                        BigDecimal.ZERO
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-not-found-1"
        ))
                .thenReturn(snapshot);

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        assertEquals(
                StartupReconciliationResult.INCONSISTENT,
                service.recover()
        );
    }

    @Test
    void allRecoverableOrdersAreCheckedEvenWhenFirstIsUnresolved() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderEntity first =
                order("recovery-first-new");

        OrderEntity second =
                order("recovery-second-filled");

        when(journalService.findRecoverableOrders())
                .thenReturn(
                        List.of(
                                first,
                                second
                        )
                );

        ExchangeOrderSnapshot firstSnapshot =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "recovery-first-new",
                        100L,
                        ExchangeOrderStatus.NEW,
                        BigDecimal.ZERO
                );

        ExchangeOrderSnapshot secondSnapshot =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "recovery-second-filled",
                        200L,
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.01")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-first-new"
        ))
                .thenReturn(firstSnapshot);

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-second-filled"
        ))
                .thenReturn(secondSnapshot);

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        StartupReconciliationResult result =
                service.recover();

        assertEquals(
                StartupReconciliationResult.INCONSISTENT,
                result
        );

        verify(queryClient)
                .getOrder(
                        "ETHUSDT",
                        "recovery-first-new"
                );

        verify(queryClient)
                .getOrder(
                        "ETHUSDT",
                        "recovery-second-filled"
                );

        verify(journalService)
                .recordExchangeSnapshot(
                        "recovery-first-new",
                        firstSnapshot
                );

        verify(journalService)
                .recordExchangeSnapshot(
                        "recovery-second-filled",
                        secondSnapshot
                );
    }

    @Test
    void unavailableHasPriorityOverInconsistent() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderEntity first =
                order("recovery-new");

        OrderEntity second =
                order("recovery-unknown");

        when(journalService.findRecoverableOrders())
                .thenReturn(
                        List.of(
                                first,
                                second
                        )
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-new"
        ))
                .thenReturn(
                        new ExchangeOrderSnapshot(
                                "ETHUSDT",
                                "recovery-new",
                                100L,
                                ExchangeOrderStatus.NEW,
                                BigDecimal.ZERO
                        )
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-unknown"
        ))
                .thenReturn(
                        new ExchangeOrderSnapshot(
                                "ETHUSDT",
                                "recovery-unknown",
                                null,
                                ExchangeOrderStatus.UNKNOWN,
                                BigDecimal.ZERO
                        )
                );

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        assertEquals(
                StartupReconciliationResult.UNAVAILABLE,
                service.recover()
        );

        verify(queryClient, times(2))
                .getOrder(
                        eq("ETHUSDT"),
                        anyString()
                );
    }

    @Test
    void queryFailureDoesNotPreventCheckingRemainingOrders() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderEntity first =
                order("recovery-query-failure");

        OrderEntity second =
                order("recovery-after-failure");

        when(journalService.findRecoverableOrders())
                .thenReturn(
                        List.of(
                                first,
                                second
                        )
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-query-failure"
        ))
                .thenThrow(
                        new RuntimeException(
                                "network failure"
                        )
                );

        ExchangeOrderSnapshot secondSnapshot =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "recovery-after-failure",
                        200L,
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.01")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "recovery-after-failure"
        ))
                .thenReturn(secondSnapshot);

        OrderRecoveryService service =
                new OrderRecoveryService(
                        journalService,
                        queryClient
                );

        assertEquals(
                StartupReconciliationResult.UNAVAILABLE,
                service.recover()
        );

        verify(queryClient)
                .getOrder(
                        "ETHUSDT",
                        "recovery-after-failure"
                );

        verify(journalService)
                .recordExchangeSnapshot(
                        "recovery-after-failure",
                        secondSnapshot
                );
    }
}