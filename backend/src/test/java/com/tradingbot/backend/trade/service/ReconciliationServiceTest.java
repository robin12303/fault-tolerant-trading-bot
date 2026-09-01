package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.fsm.TradeState;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.ExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.ExchangeOrderStatus;
import com.tradingbot.backend.trade.order.OrderJournalService;
import com.tradingbot.backend.trade.order.OrderQueryClient;
import com.tradingbot.backend.trade.position.PositionRepository;
import com.tradingbot.backend.trade.position.PositionStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReconciliationServiceTest {

    private ExchangeOrderSnapshot snapshot(
            ExchangeOrderStatus status,
            BigDecimal executedQty
    ) {
        return new ExchangeOrderSnapshot(
                "ETHUSDT",
                "bot-123",
                12345L,
                status,
                executedQty
        );
    }

    private TradeStateMachine createUnknownFsm() {

        TradeStateMachine fsm =
                new TradeStateMachine();

        // READY -> BUYING
        fsm.tryStartBuying();

        // BUYING -> UNKNOWN
        fsm.markBuyUnknown();

        return fsm;
    }

    @Test
    void rejectedWithExecutedQtyThrowsException() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.REJECTED,
                                new BigDecimal("0.01")
                        )
                );

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );
    }

    @Test
    void filledWithZeroExecutedQtyThrowsException() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.FILLED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );
    }

    @Test
    void filledOrderReconcilesToBought() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.FILLED,
                                new BigDecimal("0.05")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.BOUGHT,
                fsm.getState()
        );

        assertEquals(
                new BigDecimal("0.05"),
                positionStore
                        .getPosition("ETHUSDT")
                        .baseQty()
        );
    }

    @Test
    void rejectedOrderReconcilesToReady() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.REJECTED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void partiallyFilledWithZeroQtyThrowsException() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.PARTIALLY_FILLED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void canceledWithoutFillReconcilesToReady() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.CANCELED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void partiallyFilledStoresExposureAndStaysUnknown() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.PARTIALLY_FILLED,
                                new BigDecimal("0.03")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertEquals(
                new BigDecimal("0.03"),
                positionStore
                        .getPosition("ETHUSDT")
                        .baseQty()
        );
    }

    @Test
    void canceledWithPartialFillStaysUnknown() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.CANCELED,
                                new BigDecimal("0.02")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertEquals(
                new BigDecimal("0.02"),
                positionStore
                        .getPosition("ETHUSDT")
                        .baseQty()
        );
    }

    @Test
    void notFoundOrderStaysUnknown() {

        TradeStateMachine fsm =
                createUnknownFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.NOT_FOUND,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    private TradeStateMachine createSubmittedFsm() {

        TradeStateMachine fsm =
                new TradeStateMachine();

        // READY -> BUYING
        fsm.tryStartBuying();

        // BUYING -> BUY_SUBMITTED
        fsm.markBuySubmitted();

        return fsm;
    }

    @Test
    void filledOrderFromSubmittedReconcilesToBought() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.FILLED,
                                new BigDecimal("0.05")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.BOUGHT,
                fsm.getState()
        );

        assertEquals(
                new BigDecimal("0.05"),
                positionStore
                        .getPosition("ETHUSDT")
                        .baseQty()
        );
    }

    @Test
    void newOrderFromSubmittedStaysSubmitted() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.NEW,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.BUY_SUBMITTED,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void canceledWithPartialFillFromSubmittedBecomesUnknown() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.CANCELED,
                                new BigDecimal("0.02")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertEquals(
                new BigDecimal("0.02"),
                positionStore
                        .getPosition("ETHUSDT")
                        .baseQty()
        );
    }

    @Test
    void unknownOrderFromSubmittedBecomesUnknown() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.UNKNOWN,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }


    @Test
    void partiallyFilledOrderFromSubmittedStoresExposureAndStaysSubmitted() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.PARTIALLY_FILLED,
                                new BigDecimal("0.03")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.BUY_SUBMITTED,
                fsm.getState()
        );

        assertEquals(
                new BigDecimal("0.03"),
                positionStore
                        .getPosition("ETHUSDT")
                        .baseQty()
        );
    }

    @Test
    void canceledWithoutFillFromSubmittedReturnsToReady() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(
                        snapshot(
                                ExchangeOrderStatus.CANCELED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void queryFailureFromSubmittedBecomesUnknownAndRethrows() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenThrow(
                        new RuntimeException(
                                "network failure"
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        mock(OrderJournalService.class)
                );
        assertThrows(
                RuntimeException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void filledOrderPersistsSnapshotBeforeReconciliation() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        ExchangeOrderSnapshot filled =
                snapshot(
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.05")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(filled);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "bot-123",
                        filled
                );

        assertEquals(
                TradeState.BOUGHT,
                fsm.getState()
        );
    }
    @Test
    void journalFailurePreventsPositionAndBecomesUnknown() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        ExchangeOrderSnapshot filled =
                snapshot(
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.05")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(filled);

        when(journalService.recordExchangeSnapshot(
                "bot-123",
                filled
        ))
                .thenThrow(
                        new RuntimeException(
                                "database failure"
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                RuntimeException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "bot-123",
                        filled
                );
    }

    @Test
    void positionPersistenceFailureAfterFilledBecomesUnknownAndRethrows() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot filled =
                snapshot(
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.05")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(filled);

        doThrow(
                new RuntimeException(
                        "position persistence failure"
                )
        )
                .when(positionStore)
                .setPosition(
                        "ETHUSDT",
                        new BigDecimal("0.05")
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                RuntimeException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "bot-123",
                        filled
                );

        verify(positionStore)
                .setPosition(
                        "ETHUSDT",
                        new BigDecimal("0.05")
                );
    }

    @Test
    void positionPersistenceFailureAfterPartialFillBecomesUnknownAndRethrows() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot partial =
                snapshot(
                        ExchangeOrderStatus.PARTIALLY_FILLED,
                        new BigDecimal("0.03")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(partial);

        doThrow(
                new RuntimeException(
                        "position persistence failure"
                )
        )
                .when(positionStore)
                .setPosition(
                        "ETHUSDT",
                        new BigDecimal("0.03")
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                RuntimeException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "bot-123",
                        partial
                );

        verify(positionStore)
                .setPosition(
                        "ETHUSDT",
                        new BigDecimal("0.03")
                );
    }

    @Test
    void mismatchedClientOrderIdBecomesUnknownAndDoesNotTouchPosition() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot wrongOrder =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "some-other-order",
                        12345L,
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.05")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(wrongOrder);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verifyNoInteractions(
                journalService
        );

        verifyNoInteractions(
                positionStore
        );
    }

    @Test
    void mismatchedSymbolBecomesUnknownAndDoesNotTouchPosition() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot wrongOrder =
                new ExchangeOrderSnapshot(
                        "BTCUSDT",
                        "bot-123",
                        12345L,
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("0.05")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(wrongOrder);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verifyNoInteractions(
                journalService
        );

        verifyNoInteractions(
                positionStore
        );
    }

    @Test
    void nullSnapshotBecomesUnknownAndDoesNotTouchLocalState() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(null);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(positionStore);
    }

    @Test
    void nullStatusBecomesUnknownAndDoesNotTouchLocalState() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot broken =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "bot-123",
                        12345L,
                        null,
                        BigDecimal.ZERO
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(broken);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(positionStore);
    }

    @Test
    void negativeExecutedQtyBecomesUnknownAndDoesNotTouchLocalState() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot broken =
                new ExchangeOrderSnapshot(
                        "ETHUSDT",
                        "bot-123",
                        12345L,
                        ExchangeOrderStatus.FILLED,
                        new BigDecimal("-0.01")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(broken);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy(
                        "ETHUSDT",
                        "bot-123"
                )
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verifyNoInteractions(journalService);
        verifyNoInteractions(positionStore);
    }

    @Test
    void expiredWithoutFillFromSubmittedReturnsToReady() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot expired =
                snapshot(
                        ExchangeOrderStatus.EXPIRED,
                        BigDecimal.ZERO
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(expired);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "bot-123",
                        expired
                );

        verifyNoInteractions(positionStore);
    }

    @Test
    void expiredWithPartialFillFromSubmittedStoresExposureAndBecomesUnknown() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot expired =
                snapshot(
                        ExchangeOrderStatus.EXPIRED,
                        new BigDecimal("0.02")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(expired);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(positionStore)
                .setPosition(
                        "ETHUSDT",
                        new BigDecimal("0.02")
                );

        verify(journalService)
                .recordExchangeSnapshot(
                        "bot-123",
                        expired
                );
    }

    @Test
    void expiredInMatchWithoutFillFromSubmittedReturnsToReady() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot expired =
                snapshot(
                        ExchangeOrderStatus.EXPIRED_IN_MATCH,
                        BigDecimal.ZERO
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(expired);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        verifyNoInteractions(positionStore);
    }

    @Test
    void expiredInMatchWithPartialFillBecomesUnknown() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot expired =
                snapshot(
                        ExchangeOrderStatus.EXPIRED_IN_MATCH,
                        new BigDecimal("0.01")
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(expired);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(positionStore)
                .setPosition(
                        "ETHUSDT",
                        new BigDecimal("0.01")
                );
    }

    @Test
    void notFoundFromSubmittedBecomesUnknown() {

        TradeStateMachine fsm =
                createSubmittedFsm();

        OrderQueryClient queryClient =
                mock(OrderQueryClient.class);

        OrderJournalService journalService =
                mock(OrderJournalService.class);

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangeOrderSnapshot notFound =
                snapshot(
                        ExchangeOrderStatus.NOT_FOUND,
                        BigDecimal.ZERO
                );

        when(queryClient.getOrder(
                "ETHUSDT",
                "bot-123"
        ))
                .thenReturn(notFound);

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryClient,
                        positionStore,
                        journalService
                );

        service.reconcileBuy(
                "ETHUSDT",
                "bot-123"
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(journalService)
                .recordExchangeSnapshot(
                        "bot-123",
                        notFound
                );

        verifyNoInteractions(positionStore);
    }
}