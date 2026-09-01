package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.exchange.config.TradingExecutionProperties;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.OrderRequest;
import com.tradingbot.backend.trade.order.OrderSubmissionCoordinator;
import com.tradingbot.backend.trade.order.OrderSubmissionResult;
import org.springframework.stereotype.Service;

@Service
public class BuyOrderExecutionService {

    private final TradeStateMachine fsm;
    private final OrderSubmissionCoordinator coordinator;
    private final TradingExecutionProperties executionProperties;


    public BuyOrderExecutionService(
            TradeStateMachine fsm,
            OrderSubmissionCoordinator coordinator,
            TradingExecutionProperties executionProperties
    ) {
        this.fsm = fsm;
        this.coordinator = coordinator;
        this.executionProperties = executionProperties;
    }

    public BuyExecutionResult submit(
            OrderRequest request
    ) {

        if (!executionProperties.enabled()) {
            return BuyExecutionResult.EXECUTION_DISABLED;
        }

        if (!fsm.tryStartBuying()) {
            return BuyExecutionResult.FSM_NOT_READY;
        }

        OrderSubmissionResult result;

        try {

            result =
                    coordinator.submit(request);

        } catch (RuntimeException e) {

            fsm.markBuyUnknown();

            throw e;
        }

        if (result == null
                || result.status() == null) {

            fsm.markBuyUnknown();

            throw new IllegalStateException(
                    "Order submission result is invalid"
            );
        }

        return switch (result.status()) {

            case ACCEPTED -> {

                fsm.markBuySubmitted();

                yield BuyExecutionResult.ACCEPTED;
            }

            case REJECTED -> {

                fsm.markBuyRejected();

                yield BuyExecutionResult.REJECTED;
            }

            case RATE_LIMITED -> {

                fsm.markBuyUnknown();

                yield BuyExecutionResult.RATE_LIMITED;
            }

            case UNKNOWN -> {

                fsm.markBuyUnknown();

                yield BuyExecutionResult.UNKNOWN;
            }
        };
    }
}