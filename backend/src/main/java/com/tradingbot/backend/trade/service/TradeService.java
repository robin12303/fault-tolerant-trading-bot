package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.strategy.dto.EntrySignalResult;
import com.tradingbot.backend.strategy.service.StrategyService;
import com.tradingbot.backend.trade.exchange.config.TradingOrderProperties;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.order.ClientOrderIdGenerator;
import com.tradingbot.backend.trade.order.OrderRequest;
import com.tradingbot.backend.trade.order.OrderSide;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private final StrategyService strategyService;
    private final BuyOrderExecutionService buyOrderExecutionService;
    private final ClientOrderIdGenerator clientOrderIdGenerator;
    private final TradingOrderProperties orderProperties;
    private final BotStateMachine botStateMachine;

    public TradeService(
            StrategyService strategyService,
            BuyOrderExecutionService buyOrderExecutionService,
            ClientOrderIdGenerator clientOrderIdGenerator,
            TradingOrderProperties orderProperties,
            BotStateMachine botStateMachine
    ) {
        this.strategyService = strategyService;
        this.buyOrderExecutionService =
                buyOrderExecutionService;
        this.clientOrderIdGenerator =
                clientOrderIdGenerator;
        this.orderProperties =
                orderProperties;
        this.botStateMachine =
                botStateMachine;
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

        String clientOrderId =
                clientOrderIdGenerator.next();

        OrderRequest request =
                new OrderRequest(
                        symbol,
                        OrderSide.BUY,
                        orderProperties.buyQuoteQuantity(),
                        clientOrderId
                );

        buyOrderExecutionService.submit(request);
    }
}