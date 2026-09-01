package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.position.PositionStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StartupRecoveryTest {
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

        verify(positionStore).loadFromDatabase();

        verify(reconciliationService).reconcile();

        verify(botStateMachine, never()).halt();
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

        doThrow(new RuntimeException("DB recovery failed"))
                .when(positionStore)
                .loadFromDatabase();

        assertThrows(
                RuntimeException.class,
                () -> startupRecovery.run(args)
        );

        verify(botStateMachine).halt();
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

        doThrow(new RuntimeException("Reconciliation failed"))
                .when(reconciliationService)
                .reconcile();

        assertThrows(
                RuntimeException.class,
                () -> startupRecovery.run(args)
        );

        verify(positionStore).loadFromDatabase();

        verify(reconciliationService).reconcile();

        verify(botStateMachine).halt();
    }

}