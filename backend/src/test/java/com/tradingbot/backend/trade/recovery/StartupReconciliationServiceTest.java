package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.exchange.config.TradingSafetyProperties;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class StartupReconciliationServiceTest {

    @Test
    void nonDedicatedAccountHaltsWithoutReconciliation() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        TradingSafetyProperties safetyProperties =
                new TradingSafetyProperties(false);

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        checker,
                        safetyProperties
                );

        service.reconcile();

        verify(botStateMachine).halt();

        verify(botStateMachine, never())
                .activate();

        verify(checker, never())
                .check();
    }


    @Test
    void consistentResultActivatesBot() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(checker.check())
                .thenReturn(
                        StartupReconciliationResult.CONSISTENT
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        checker,
                        new TradingSafetyProperties(true)
                );
        service.reconcile();

        verify(checker).check();

        verify(botStateMachine).activate();

        verify(botStateMachine, never())
                .halt();
    }

    @Test
    void inconsistentResultHaltsBot() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(checker.check())
                .thenReturn(
                        StartupReconciliationResult.INCONSISTENT
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        checker,
                        new TradingSafetyProperties(true)
                );
        service.reconcile();

        verify(checker).check();

        verify(botStateMachine).halt();

        verify(botStateMachine, never())
                .activate();
    }

    @Test
    void unavailableResultHaltsBot() {

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        when(checker.check())
                .thenReturn(
                        StartupReconciliationResult.UNAVAILABLE
                );

        StartupReconciliationService service =
                new StartupReconciliationService(
                        botStateMachine,
                        checker,
                        new TradingSafetyProperties(true)
                );

        service.reconcile();

        verify(checker).check();

        verify(botStateMachine).halt();

        verify(botStateMachine, never())
                .activate();
    }
}