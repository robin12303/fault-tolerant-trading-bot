package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.exchange.config.TradingSafetyProperties;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class StartupReconciliationServiceTest {

    @Test
    void nonDedicatedAccountHaltsWithoutRecovery() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        OrderRecoveryService orderRecoveryService =
                mock(OrderRecoveryService.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        new TradingSafetyProperties(false)
                );

        service.reconcile();

        verify(botStateMachine)
                .halt();

        verify(botStateMachine, never())
                .activate();

        verifyNoInteractions(
                orderRecoveryService,
                checker
        );
    }

    @Test
    void consistentOrderAndPositionActivatesBot() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        OrderRecoveryService orderRecoveryService =
                mock(OrderRecoveryService.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(orderRecoveryService.recover())
                .thenReturn(
                        StartupReconciliationResult.CONSISTENT
                );

        when(checker.check())
                .thenReturn(
                        StartupReconciliationResult.CONSISTENT
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        new TradingSafetyProperties(true)
                );

        service.reconcile();

        verify(orderRecoveryService)
                .recover();

        verify(checker)
                .check();

        verify(botStateMachine)
                .activate();

        verify(botStateMachine, never())
                .halt();
    }

    @Test
    void inconsistentOrderHaltsBeforePositionCheck() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        OrderRecoveryService orderRecoveryService =
                mock(OrderRecoveryService.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(orderRecoveryService.recover())
                .thenReturn(
                        StartupReconciliationResult.INCONSISTENT
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        new TradingSafetyProperties(true)
                );

        service.reconcile();

        verify(orderRecoveryService)
                .recover();

        verify(checker, never())
                .check();

        verify(botStateMachine)
                .halt();

        verify(botStateMachine, never())
                .activate();
    }

    @Test
    void unavailableOrderRecoveryHaltsBeforePositionCheck() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        OrderRecoveryService orderRecoveryService =
                mock(OrderRecoveryService.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(orderRecoveryService.recover())
                .thenReturn(
                        StartupReconciliationResult.UNAVAILABLE
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        new TradingSafetyProperties(true)
                );

        service.reconcile();

        verify(orderRecoveryService)
                .recover();

        verify(checker, never())
                .check();

        verify(botStateMachine)
                .halt();

        verify(botStateMachine, never())
                .activate();
    }

    @Test
    void inconsistentPositionHaltsBot() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        OrderRecoveryService orderRecoveryService =
                mock(OrderRecoveryService.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(orderRecoveryService.recover())
                .thenReturn(
                        StartupReconciliationResult.CONSISTENT
                );

        when(checker.check())
                .thenReturn(
                        StartupReconciliationResult.INCONSISTENT
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        new TradingSafetyProperties(true)
                );

        service.reconcile();

        verify(orderRecoveryService)
                .recover();

        verify(checker)
                .check();

        verify(botStateMachine)
                .halt();

        verify(botStateMachine, never())
                .activate();
    }

    @Test
    void unavailablePositionCheckHaltsBot() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        OrderRecoveryService orderRecoveryService =
                mock(OrderRecoveryService.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(orderRecoveryService.recover())
                .thenReturn(
                        StartupReconciliationResult.CONSISTENT
                );

        when(checker.check())
                .thenReturn(
                        StartupReconciliationResult.UNAVAILABLE
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        new TradingSafetyProperties(true)
                );

        service.reconcile();

        verify(orderRecoveryService)
                .recover();

        verify(checker)
                .check();

        verify(botStateMachine)
                .halt();

        verify(botStateMachine, never())
                .activate();
    }
}