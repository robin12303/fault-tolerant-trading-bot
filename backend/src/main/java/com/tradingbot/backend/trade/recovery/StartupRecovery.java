package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.position.PositionStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRecovery implements ApplicationRunner {

    private final PositionStore positionStore;
    private final BotStateMachine botStateMachine;
    private final StartupReconciliationService reconciliationService;


    public StartupRecovery(
            PositionStore positionStore,
            BotStateMachine botStateMachine,
            StartupReconciliationService reconciliationService
    ) {
        this.positionStore = positionStore;
        this.botStateMachine = botStateMachine;
        this.reconciliationService = reconciliationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            positionStore.loadFromDatabase();

            reconciliationService.reconcile();

        } catch (RuntimeException e) {
            botStateMachine.halt();
            throw e;
        }
    }
}