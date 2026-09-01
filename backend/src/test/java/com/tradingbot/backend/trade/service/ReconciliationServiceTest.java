package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.fsm.TradeState;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.MockExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.MockExchangeOrderStatus;
import com.tradingbot.backend.trade.position.PositionRepository;
import com.tradingbot.backend.trade.position.PositionStore;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReconciliationServiceTest {

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

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.REJECTED,
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
                        queryService,
                        positionStore
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy("ETHUSDT")
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

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.FILLED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService,
                        positionStore

                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy("ETHUSDT")
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

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.FILLED,
                                new BigDecimal("0.05")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService,
                        positionStore
                );

        service.reconcileBuy("ETHUSDT");

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

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.REJECTED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService,
                        positionStore
                );

        service.reconcileBuy("ETHUSDT");

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void partiallyFilledWithZeroQtyThrowsException(){
        TradeStateMachine fsm =
                createUnknownFsm();

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);


        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.PARTIALLY_FILLED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService
                        ,
                        positionStore
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.reconcileBuy("ETHUSDT")
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

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.CANCELED,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService
                        ,
                        positionStore
                );

        service.reconcileBuy("ETHUSDT");

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }

    @Test
    void partiallyFilledStoresExposureAndStaysUnknown(){
        TradeStateMachine fsm =
                createUnknownFsm();
        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);
        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);
        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.PARTIALLY_FILLED,
                                new BigDecimal("0.03")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService,
                        positionStore
                );

        service.reconcileBuy("ETHUSDT");

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

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.CANCELED,
                                new BigDecimal("0.02")
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService,
                        positionStore
                );

        service.reconcileBuy("ETHUSDT");

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

        MockExchangeQueryService queryService =
                mock(MockExchangeQueryService.class);

        PositionRepository positionRepository =
                mock(PositionRepository.class);

        PositionStore positionStore =
                new PositionStore(positionRepository);

        when(queryService.queryBuyOrder("ETHUSDT"))
                .thenReturn(
                        new MockExchangeOrderSnapshot(
                                MockExchangeOrderStatus.NOT_FOUND,
                                BigDecimal.ZERO
                        )
                );

        ReconciliationService service =
                new ReconciliationService(
                        fsm,
                        queryService,
                        positionStore
                );

        service.reconcileBuy("ETHUSDT");

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        assertNull(
                positionStore.getPosition("ETHUSDT")
        );
    }
}