package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.fsm.TradeState;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.MockExchangeOrderSnapshot;
import com.tradingbot.backend.trade.position.PositionStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReconciliationService {

    private final TradeStateMachine fsm;
    private final MockExchangeQueryService exchangeQueryService;
    private final PositionStore positionStore;
    public ReconciliationService(
            TradeStateMachine fsm,
            MockExchangeQueryService exchangeQueryService,
            PositionStore positionStore
    ) {
        this.fsm = fsm;
        this.exchangeQueryService = exchangeQueryService;
        this.positionStore = positionStore;
    }

    public void reconcileBuy(String symbol) {

        if (fsm.getState() != TradeState.UNKNOWN) {
            throw new IllegalStateException(
                    "Reconciliation requires UNKNOWN state"
            );
        }

        MockExchangeOrderSnapshot order =
                exchangeQueryService.queryBuyOrder(symbol);

        switch (order.status()) {

            case FILLED -> {

                if (order.executedQty()
                        .compareTo(BigDecimal.ZERO) <= 0) {

                    throw new IllegalStateException(
                            "FILLED but executedQty is zero"
                    );
                }

                positionStore.setPosition(
                        symbol,
                        order.executedQty()
                );

                fsm.reconcileToBought();
            }

            case REJECTED -> {

                if (order.executedQty()
                        .compareTo(BigDecimal.ZERO) != 0) {

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

                    positionStore.setPosition(
                            symbol,
                            order.executedQty()
                    );

                    // 일부 체결됨.
                    // 실제 노출은 기록하지만
                    // 처리 정책이 아직 결정되지 않았으므로
                    // FSM은 UNKNOWN 유지.
                }
            }

            case PARTIALLY_FILLED -> {

                if (order.executedQty()
                        .compareTo(BigDecimal.ZERO) <= 0) {

                    throw new IllegalStateException(
                            "PARTIALLY_FILLED but executedQty is zero"
                    );
                }

                positionStore.setPosition(
                        symbol,
                        order.executedQty()
                );

                // UNKNOWN 유지
            }

            case NOT_FOUND, UNKNOWN -> {
                // 아무것도 확정할 수 없음.
                // UNKNOWN 유지.
            }
        }
    }
}