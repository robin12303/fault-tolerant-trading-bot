package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.strategy.dto.EntrySignalResult;
import com.tradingbot.backend.strategy.service.StrategyService;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.MockOrderResult;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private final StrategyService strategyService;
    private final TradeStateMachine fsm;
    private final MockOrderService mockOrderService;
    private final BotStateMachine botStateMachine;
    public TradeService(
            StrategyService strategyService,
            TradeStateMachine fsm,
            MockOrderService mockOrderService,
            BotStateMachine botStateMachine
    ) {
        this.strategyService = strategyService;
        this.fsm = fsm;
        this.mockOrderService = mockOrderService;
        this.botStateMachine = botStateMachine;
    }

    public void checkAndBuy(String symbol) {

        if (!botStateMachine.isActive()) {
            return;
        }

        EntrySignalResult signal =
                strategyService.evaluate(symbol);

        if (!signal.entrySignal()) {
            return;
        }

        if (!fsm.tryStartBuying()) {
            return;
        }

        try {

            MockOrderResult result =
                    mockOrderService.buy(symbol);

            switch (result) {

                case FILLED ->
                        fsm.markBought();

                case REJECTED ->
                        fsm.markBuyRejected();

                case TIMEOUT ->
                        fsm.markBuyUnknown();
            }

        } catch (Exception e) {

            // 예상하지 못한 오류도
            // 주문 결과를 확정할 수 없다고 판단
            fsm.markBuyUnknown();

            throw e;
        }
    }
}