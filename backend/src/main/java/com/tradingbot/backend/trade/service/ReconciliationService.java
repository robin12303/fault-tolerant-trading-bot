package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.fsm.TradeState;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.OrderJournalService;
import com.tradingbot.backend.trade.position.PositionStore;
import org.springframework.stereotype.Service;
import com.tradingbot.backend.trade.order.ExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.OrderQueryClient;
import java.math.BigDecimal;

@Service
public class ReconciliationService {

    private final TradeStateMachine fsm;
    private final OrderQueryClient orderQueryClient;
    private final PositionStore positionStore;
    private final OrderJournalService orderJournalService;


    public ReconciliationService(
            TradeStateMachine fsm,
            OrderQueryClient orderQueryClient,
            PositionStore positionStore,
            OrderJournalService orderJournalService
    ) {
        this.fsm = fsm;
        this.orderQueryClient = orderQueryClient;
        this.positionStore = positionStore;
        this.orderJournalService = orderJournalService;
    }
    public void reconcileBuy(
            String symbol,
            String clientOrderId
    ) {

        TradeState current =
                fsm.getState();

        if (current != TradeState.UNKNOWN
                && current != TradeState.BUY_SUBMITTED) {

            throw new IllegalStateException(
                    "Reconciliation requires UNKNOWN or BUY_SUBMITTED state"
            );
        }

        ExchangeOrderSnapshot order;

        try {

            order =
                    orderQueryClient.getOrder(
                            symbol,
                            clientOrderId
                    );

        } catch (RuntimeException e) {

            fsm.reconcileToUnknown();

            throw e;
        }

        validateSnapshotOrUnknown(
                symbol,
                clientOrderId,
                order
        );

        try {

            orderJournalService.recordExchangeSnapshot(
                    clientOrderId,
                    order
            );

        } catch (RuntimeException e) {

            fsm.reconcileToUnknown();

            throw e;
        }

        switch (order.status()) {

            case FILLED -> {

                if (order.executedQty()
                        .compareTo(BigDecimal.ZERO) <= 0) {

                    fsm.reconcileToUnknown();

                    throw new IllegalStateException(
                            "FILLED but executedQty is zero"
                    );
                }

                setPositionOrUnknown(
                        symbol,
                        order.executedQty()
                );

                fsm.reconcileToBought();
            }

            case REJECTED -> {

                if (order.executedQty()
                        .compareTo(BigDecimal.ZERO) != 0) {

                    fsm.reconcileToUnknown();

                    throw new IllegalStateException(
                            "REJECTED but executedQty is not zero"
                    );
                }

                fsm.reconcileToReady();
            }

            case CANCELED -> {

                if (order.executedQty()
                        .compareTo(BigDecimal.ZERO) == 0) {

                    fsm.reconcileToReady();

                } else {

                    setPositionOrUnknown(
                            symbol,
                            order.executedQty()
                    );

                    fsm.reconcileToUnknown();
                }
            }

            case PARTIALLY_FILLED -> {

                if (order.executedQty()
                        .compareTo(BigDecimal.ZERO) <= 0) {

                    fsm.reconcileToUnknown();

                    throw new IllegalStateException(
                            "PARTIALLY_FILLED but executedQty is zero"
                    );
                }

                setPositionOrUnknown(
                        symbol,
                        order.executedQty()
                );

                // 현재 FSM 상태 유지
            }

            case EXPIRED,
                 EXPIRED_IN_MATCH ->

                    reconcileTerminalExpiration(
                            symbol,
                            order.executedQty()
                    );

            case NEW -> {
                // 거래소가 주문 존재를 확인함.
                // 현재 FSM 상태 유지.
            }
            case UNKNOWN,
                 NOT_FOUND -> {
                fsm.reconcileToUnknown();
            }
        }
    }

    private void setPositionOrUnknown(
            String symbol,
            BigDecimal executedQty
    ) {

        try {

            positionStore.setPosition(
                    symbol,
                    executedQty
            );

        } catch (RuntimeException e) {

            fsm.reconcileToUnknown();

            throw e;
        }
    }

    private void validateSnapshotOrUnknown(
            String expectedSymbol,
            String expectedClientOrderId,
            ExchangeOrderSnapshot order
    ) {

        if (order == null) {

            fsm.reconcileToUnknown();

            throw new IllegalStateException(
                    "Exchange order snapshot must not be null"
            );
        }

        if (order.status() == null) {

            fsm.reconcileToUnknown();

            throw new IllegalStateException(
                    "Exchange order status must not be null"
            );
        }

        if (order.executedQty() == null) {

            fsm.reconcileToUnknown();

            throw new IllegalStateException(
                    "Exchange executedQty must not be null"
            );
        }

        if (order.executedQty()
                .compareTo(BigDecimal.ZERO) < 0) {

            fsm.reconcileToUnknown();

            throw new IllegalStateException(
                    "Exchange executedQty must not be negative"
            );
        }

        if (!expectedSymbol.equals(
                order.symbol()
        )) {

            fsm.reconcileToUnknown();

            throw new IllegalStateException(
                    "Order symbol mismatch"
            );
        }

        if (!expectedClientOrderId.equals(
                order.clientOrderId()
        )) {

            fsm.reconcileToUnknown();

            throw new IllegalStateException(
                    "Order clientOrderId mismatch"
            );
        }
    }

    private void reconcileTerminalExpiration(
            String symbol,
            BigDecimal executedQty
    ) {

        if (executedQty.compareTo(BigDecimal.ZERO) == 0) {
            fsm.reconcileToReady();
            return;
        }

        setPositionOrUnknown(
                symbol,
                executedQty
        );

        fsm.reconcileToUnknown();
    }
}