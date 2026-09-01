package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.strategy.dto.EntrySignalResult;
import com.tradingbot.backend.strategy.service.StrategyService;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.fsm.TradeState;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.MockOrderResult;
import org.junit.jupiter.api.Test;

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

        TradeStateMachine fsm =
                new TradeStateMachine();

        MockOrderService orderService =
                mock(MockOrderService.class);

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        when(botStateMachine.isActive())
                .thenReturn(false);

        TradeService tradeService =
                new TradeService(
                        strategyService,
                        fsm,
                        orderService,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        verify(strategyService, never())
                .evaluate(anyString());

        verify(orderService, never())
                .buy(anyString());
    }

    @Test
    void filledBuyBecomesBought() {

        StrategyService strategyService =
                mock(StrategyService.class);

        MockOrderService orderService =
                mock(MockOrderService.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        when(botStateMachine.isActive())
                .thenReturn(true);

        when(strategyService.evaluate("ETHUSDT"))
                .thenReturn(trueSignal());

        when(orderService.buy("ETHUSDT"))
                .thenReturn(MockOrderResult.FILLED);

        TradeService tradeService =
                new TradeService(
                        strategyService,
                        fsm,
                        orderService,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        assertEquals(
                TradeState.BOUGHT,
                fsm.getState()
        );

        verify(orderService, times(1))
                .buy("ETHUSDT");
    }

    @Test
    void rejectedBuyReturnsToReady() {

        StrategyService strategyService =
                mock(StrategyService.class);

        MockOrderService orderService =
                mock(MockOrderService.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        when(botStateMachine.isActive())
                .thenReturn(true);

        when(strategyService.evaluate("ETHUSDT"))
                .thenReturn(trueSignal());

        when(orderService.buy("ETHUSDT"))
                .thenReturn(MockOrderResult.REJECTED);

        TradeService tradeService =
                new TradeService(
                        strategyService,
                        fsm,
                        orderService,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        verify(orderService, times(1))
                .buy("ETHUSDT");
    }

    @Test
    void timeoutBuyBecomesUnknown() {

        StrategyService strategyService =
                mock(StrategyService.class);

        MockOrderService orderService =
                mock(MockOrderService.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        when(botStateMachine.isActive())
                .thenReturn(true);

        when(strategyService.evaluate("ETHUSDT"))
                .thenReturn(trueSignal());

        when(orderService.buy("ETHUSDT"))
                .thenReturn(MockOrderResult.TIMEOUT);

        TradeService tradeService =
                new TradeService(
                        strategyService,
                        fsm,
                        orderService,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(orderService, times(1))
                .buy("ETHUSDT");
    }

    @Test
    void falseSignalDoesNotPlaceOrder() {

        StrategyService strategyService =
                mock(StrategyService.class);

        MockOrderService orderService =
                mock(MockOrderService.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

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
                        fsm,
                        orderService,
                        botStateMachine
                );

        tradeService.checkAndBuy("ETHUSDT");

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        verify(strategyService, times(1))
                .evaluate("ETHUSDT");

        verify(orderService, never())
                .buy(anyString());
    }
}