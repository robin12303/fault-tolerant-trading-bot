package com.tradingbot.backend.trade.order;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderSubmissionCoordinatorTest {

    @Test
    void journalIsWrittenBeforeOrderSubmission() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderClient orderClient =
                mock(OrderClient.class);

        OrderSubmissionCoordinator coordinator =
                new OrderSubmissionCoordinator(
                        journalService,
                        orderClient
                );

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-123"
                );

        OrderSubmissionResult result =
                new OrderSubmissionResult(
                        OrderSubmissionStatus.ACCEPTED,
                        "bot-123",
                        12345L
                );

        when(orderClient.submit(request))
                .thenReturn(result);

        OrderSubmissionResult actual =
                coordinator.submit(request);

        assertEquals(
                result,
                actual
        );

        InOrder inOrder =
                inOrder(
                        journalService,
                        orderClient
                );

        inOrder.verify(journalService)
                .recordPending(request);

        inOrder.verify(orderClient)
                .submit(request);

        inOrder.verify(journalService)
                .recordSubmissionResult(
                        "bot-123",
                        result
                );
    }

    @Test
    void journalFailurePreventsOrderSubmission() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderClient orderClient =
                mock(OrderClient.class);

        OrderSubmissionCoordinator coordinator =
                new OrderSubmissionCoordinator(
                        journalService,
                        orderClient
                );

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-123"
                );

        doThrow(
                new IllegalStateException(
                        "database failure"
                )
        )
                .when(journalService)
                .recordPending(request);

        assertThrows(
                IllegalStateException.class,
                () -> coordinator.submit(request)
        );

        verifyNoInteractions(orderClient);

        verify(
                journalService,
                never()
        ).recordSubmissionResult(
                anyString(),
                any()
        );
    }

    @Test
    void unexpectedSubmissionFailureLeavesJournalPending() {

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        OrderClient orderClient =
                mock(OrderClient.class);

        OrderSubmissionCoordinator coordinator =
                new OrderSubmissionCoordinator(
                        journalService,
                        orderClient
                );

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-123"
                );

        when(orderClient.submit(request))
                .thenThrow(
                        new RuntimeException(
                                "unexpected exchange failure"
                        )
                );

        assertThrows(
                RuntimeException.class,
                () -> coordinator.submit(request)
        );

        verify(journalService)
                .recordPending(request);

        verify(orderClient)
                .submit(request);

        verify(
                journalService,
                never()
        ).recordSubmissionResult(
                anyString(),
                any()
        );
    }
}