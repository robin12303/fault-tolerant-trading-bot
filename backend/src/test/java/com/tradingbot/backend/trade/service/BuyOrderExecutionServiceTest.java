package com.tradingbot.backend.trade.service;
import com.tradingbot.backend.trade.exchange.config.TradingExecutionProperties;
import com.tradingbot.backend.trade.fsm.TradeState;
import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.order.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
public class BuyOrderExecutionServiceTest {
    @Test
    void acceptedOrderBecomesSubmittedNotBought() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-test-123"
                );

        when(coordinator.submit(request))
                .thenReturn(
                        new OrderSubmissionResult(
                                OrderSubmissionStatus.ACCEPTED,
                                "bot-test-123",
                                12345L
                        )
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(true)
                );

        BuyExecutionResult result =
                service.submit(request);

        assertEquals(
                BuyExecutionResult.ACCEPTED,
                result
        );

        assertEquals(
                TradeState.BUY_SUBMITTED,
                fsm.getState()
        );

        verify(coordinator)
                .submit(request);
    }

    @Test
    void rejectedOrderReturnsToReady() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-test-rejected-123"
                );

        when(coordinator.submit(request))
                .thenReturn(
                        new OrderSubmissionResult(
                                OrderSubmissionStatus.REJECTED,
                                "bot-test-rejected-123",
                                null
                        )
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(true)
                );

        BuyExecutionResult result =
                service.submit(request);

        assertEquals(
                BuyExecutionResult.REJECTED,
                result
        );

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        verify(coordinator)
                .submit(request);
    }

    @Test
    void unknownOrderBecomesUnknown() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-test-unknown-123"
                );

        when(coordinator.submit(request))
                .thenReturn(
                        new OrderSubmissionResult(
                                OrderSubmissionStatus.UNKNOWN,
                                "bot-test-unknown-123",
                                null
                        )
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(true)
                );

        BuyExecutionResult result =
                service.submit(request);

        assertEquals(
                BuyExecutionResult.UNKNOWN,
                result
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(coordinator)
                .submit(request);
    }

    @Test
    void rateLimitedOrderBecomesUnknownState() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-test-rate-limited-123"
                );

        when(coordinator.submit(request))
                .thenReturn(
                        new OrderSubmissionResult(
                                OrderSubmissionStatus.RATE_LIMITED,
                                "bot-test-rate-limited-123",
                                null
                        )
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(true)
                );

        BuyExecutionResult result =
                service.submit(request);

        assertEquals(
                BuyExecutionResult.RATE_LIMITED,
                result
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(coordinator)
                .submit(request);
    }

    @Test
    void coordinatorExceptionBecomesUnknownAndRethrows() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-test-exception-123"
                );

        when(coordinator.submit(request))
                .thenThrow(
                        new RuntimeException(
                                "submission failure"
                        )
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(true)
                );

        assertThrows(
                RuntimeException.class,
                () -> service.submit(request)
        );

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );

        verify(coordinator)
                .submit(request);
    }

    @Test
    void buyingStatePreventsSecondOrderSubmission() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        // 이미 다른 주문이 BUYING 상태를 점유했다고 가정
        assertTrue(
                fsm.tryStartBuying()
        );

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-test-duplicate-123"
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(true)
                );

        BuyExecutionResult result =
                service.submit(request);

        assertEquals(
                BuyExecutionResult.FSM_NOT_READY,
                result
        );

        assertEquals(
                TradeState.BUYING,
                fsm.getState()
        );

        verifyNoInteractions(coordinator);
    }

    @Test
    void submittedStatePreventsSecondOrderSubmission() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        // 첫 주문이 이미 Binance에 접수된 상태
        assertTrue(
                fsm.tryStartBuying()
        );

        fsm.markBuySubmitted();

        assertEquals(
                TradeState.BUY_SUBMITTED,
                fsm.getState()
        );

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-test-second-123"
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(true)
                );

        BuyExecutionResult result =
                service.submit(request);

        assertEquals(
                BuyExecutionResult.FSM_NOT_READY,
                result
        );

        assertEquals(
                TradeState.BUY_SUBMITTED,
                fsm.getState()
        );

        verifyNoInteractions(coordinator);
    }

    @Test
    void disabledExecutionDoesNotTouchFsmOrCoordinator() {

        OrderSubmissionCoordinator coordinator =
                mock(OrderSubmissionCoordinator.class);

        TradeStateMachine fsm =
                new TradeStateMachine();

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.00"),
                        "bot-disabled-123"
                );

        BuyOrderExecutionService service =
                new BuyOrderExecutionService(
                        fsm,
                        coordinator,
                        new TradingExecutionProperties(false)
                );

        BuyExecutionResult result =
                service.submit(request);

        assertEquals(
                BuyExecutionResult.EXECUTION_DISABLED,
                result
        );

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );

        verifyNoInteractions(coordinator);
    }
}
