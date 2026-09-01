package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.exchange.config.TradingSafetyProperties;
import com.tradingbot.backend.trade.fsm.BotState;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.position.PositionStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StartupRecoveryTest {

    @Test
    void dedicatedAccountWithConsistentStateBecomesActive() throws Exception {

        PositionStore positionStore =
                mock(PositionStore.class);

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

        BotStateMachine botStateMachine =
                new BotStateMachine();

        TradingSafetyProperties safetyProperties =
                new TradingSafetyProperties(true);

        StartupReconciliationService reconciliationService =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        safetyProperties
                );

        StartupRecovery startupRecovery =
                new StartupRecovery(
                        positionStore,
                        botStateMachine,
                        reconciliationService
                );

        ApplicationArguments args =
                mock(ApplicationArguments.class);

        startupRecovery.run(args);

        assertEquals(
                BotState.ACTIVE,
                botStateMachine.getState()
        );

        verify(positionStore)
                .loadFromDatabase();

        verify(orderRecoveryService)
                .recover();

        verify(checker)
                .check();
    }

    @Test
    void nonDedicatedAccountEndsStartupInHaltedState() throws Exception {

        PositionStore positionStore =
                mock(PositionStore.class);

        OrderRecoveryService orderRecoveryService =
                mock(OrderRecoveryService.class);

        StartupReconciliationChecker checker =
                mock(StartupReconciliationChecker.class);

        BotStateMachine botStateMachine =
                new BotStateMachine();

        TradingSafetyProperties safetyProperties =
                new TradingSafetyProperties(false);

        StartupReconciliationService reconciliationService =
                new StartupReconciliationService(
                        botStateMachine,
                        orderRecoveryService,
                        checker,
                        safetyProperties
                );

        StartupRecovery startupRecovery =
                new StartupRecovery(
                        positionStore,
                        botStateMachine,
                        reconciliationService
                );

        ApplicationArguments args =
                mock(ApplicationArguments.class);

        startupRecovery.run(args);

        assertEquals(
                BotState.HALTED,
                botStateMachine.getState()
        );

        verify(positionStore)
                .loadFromDatabase();

        verifyNoInteractions(
                orderRecoveryService,
                checker
        );
    }

    @Test
    void successfulRecoveryLoadsDatabaseAndReconciles() throws Exception {

        PositionStore positionStore =
                mock(PositionStore.class);

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        ApplicationArguments args =
                mock(ApplicationArguments.class);

        StartupReconciliationService reconciliationService =
                mock(StartupReconciliationService.class);

        StartupRecovery startupRecovery =
                new StartupRecovery(
                        positionStore,
                        botStateMachine,
                        reconciliationService
                );

        startupRecovery.run(args);

        verify(positionStore)
                .loadFromDatabase();

        verify(reconciliationService)
                .reconcile();

        verify(botStateMachine, never())
                .halt();
    }

    @Test
    void failedRecoveryHaltsBot() {

        PositionStore positionStore =
                mock(PositionStore.class);

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        ApplicationArguments args =
                mock(ApplicationArguments.class);

        StartupReconciliationService reconciliationService =
                mock(StartupReconciliationService.class);

        StartupRecovery startupRecovery =
                new StartupRecovery(
                        positionStore,
                        botStateMachine,
                        reconciliationService
                );

        doThrow(
                new RuntimeException(
                        "DB recovery failed"
                )
        )
                .when(positionStore)
                .loadFromDatabase();

        assertThrows(
                RuntimeException.class,
                () -> startupRecovery.run(args)
        );

        verify(botStateMachine)
                .halt();

        verify(reconciliationService, never())
                .reconcile();
    }

    @Test
    void failedReconciliationHaltsBot() {

        PositionStore positionStore =
                mock(PositionStore.class);

        BotStateMachine botStateMachine =
                mock(BotStateMachine.class);

        ApplicationArguments args =
                mock(ApplicationArguments.class);

        StartupReconciliationService reconciliationService =
                mock(StartupReconciliationService.class);

        StartupRecovery startupRecovery =
                new StartupRecovery(
                        positionStore,
                        botStateMachine,
                        reconciliationService
                );

        doThrow(
                new RuntimeException(
                        "Reconciliation failed"
                )
        )
                .when(reconciliationService)
                .reconcile();

        assertThrows(
                RuntimeException.class,
                () -> startupRecovery.run(args)
        );

        verify(positionStore)
                .loadFromDatabase();

        verify(reconciliationService)
                .reconcile();

        verify(botStateMachine)
                .halt();
    }
}