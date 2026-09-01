package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.strategy.dto.EntrySignalResult;
import com.tradingbot.backend.strategy.service.StrategyService;
import com.tradingbot.backend.trade.exchange.config.TradingOrderProperties;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.order.ClientOrderIdGenerator;
import com.tradingbot.backend.trade.order.OrderRequest;
import com.tradingbot.backend.trade.order.OrderSide;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TradeServiceTest {

    private EntrySignalResult trueSignal() {

        return new EntrySignalResult(
                "ETHUSDT",
                new BigDecimal("2500"),
                new BigDecimal("2400"),
                new BigDecimal("2450"),
                new BigDecimal("80000"),
                new BigDecimal("79000"),
                true
        );
    }

    @Test
    void inactiveBotDoesNotEvaluateOrBuy() {

        StrategyService strategyService =
                mock(StrategyService.class);

        BuyOrderExecutionService executionService =
                mock(BuyOrderExecutionService.class);

        ClientOrderIdGenerator idGenerator =
                mock(ClientOrderIdGenerator.class);

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        TradingOrderProperties orderProperties =
                new TradingOrderProperties(
                        new BigDecimal("20.00")
                );

        when(botStateMachine.isActive())
                .thenReturn(false);

        TradeService tradeService =
                new TradeService(
                        strategyService,
                        executionService,
                        idGenerator,
                        orderProperties,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        verify(strategyService, never())
                .evaluate(anyString());

        verifyNoInteractions(
                executionService
        );

        verifyNoInteractions(
                idGenerator
        );
    }

    @Test
    void falseSignalDoesNotPlaceOrder() {

        StrategyService strategyService =
                mock(StrategyService.class);

        BuyOrderExecutionService executionService =
                mock(BuyOrderExecutionService.class);

        ClientOrderIdGenerator idGenerator =
                mock(ClientOrderIdGenerator.class);

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        TradingOrderProperties orderProperties =
                new TradingOrderProperties(
                        new BigDecimal("20.00")
                );

        EntrySignalResult falseSignal =
                new EntrySignalResult(
                        "ETHUSDT",
                        new BigDecimal("2400"),
                        new BigDecimal("2500"),
                        new BigDecimal("2450"),
                        new BigDecimal("78000"),
                        new BigDecimal("79000"),
                        false
                );

        when(botStateMachine.isActive())
                .thenReturn(true);

        when(strategyService.evaluate("ETHUSDT"))
                .thenReturn(falseSignal);

        TradeService tradeService =
                new TradeService(
                        strategyService,
                        executionService,
                        idGenerator,
                        orderProperties,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        verify(strategyService)
                .evaluate("ETHUSDT");

        verifyNoInteractions(
                executionService
        );

        verifyNoInteractions(
                idGenerator
        );
    }

    @Test
    void trueSignalCreatesBuyRequestAndSubmits() {

        StrategyService strategyService =
                mock(StrategyService.class);

        BuyOrderExecutionService executionService =
                mock(BuyOrderExecutionService.class);

        ClientOrderIdGenerator idGenerator =
                mock(ClientOrderIdGenerator.class);

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        TradingOrderProperties orderProperties =
                new TradingOrderProperties(
                        new BigDecimal("20.00")
                );

        when(botStateMachine.isActive())
                .thenReturn(true);

        when(strategyService.evaluate("ETHUSDT"))
                .thenReturn(trueSignal());

        when(idGenerator.next())
                .thenReturn("bot-test-123");

        TradeService tradeService =
                new TradeService(
                        strategyService,
                        executionService,
                        idGenerator,
                        orderProperties,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        ArgumentCaptor<OrderRequest> captor =
                ArgumentCaptor.forClass(
                        OrderRequest.class
                );

        verify(executionService)
                .submit(captor.capture());

        OrderRequest request =
                captor.getValue();

        assertEquals(
                "ETHUSDT",
                request.symbol()
        );

        assertEquals(
                OrderSide.BUY,
                request.side()
        );

        assertEquals(
                0,
                new BigDecimal("20.00")
                        .compareTo(
                                request.quoteQuantity()
                        )
        );

        assertEquals(
                "bot-test-123",
                request.clientOrderId()
        );

        verify(idGenerator)
                .next();
    }
}