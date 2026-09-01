package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.fsm.TradeState;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.ExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.ExchangeOrderStatus;
import com.tradingbot.backend.trade.order.OrderQueryClient;
import com.tradingbot.backend.trade.position.PositionRepository;
import com.tradingbot.backend.trade.position.PositionStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                        positionStore
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
                        positionStore
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
                        positionStore
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
                        positionStore
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
                        positionStore
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
                        positionStore
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
                        positionStore
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
                        positionStore
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
                        positionStore
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
}