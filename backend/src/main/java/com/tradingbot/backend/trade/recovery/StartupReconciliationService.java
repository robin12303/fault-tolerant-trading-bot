package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.fsm.BotStateMachine;
import org.springframework.stereotype.Service;

@Service
public class StartupReconciliationService {

    private final BotStateMachine botStateMachine;
    private final StartupReconciliationChecker checker;
    public StartupReconciliationService(
            BotStateMachine botStateMachine,
            StartupReconciliationChecker checker
    ) {
        this.botStateMachine = botStateMachine;
        this.checker = checker;
    }

    public void reconcile() {

        StartupReconciliationResult result =
                checker.check();

        switch (result) {
            case CONSISTENT ->
                    botStateMachine.activate();

            case INCONSISTENT, UNAVAILABLE ->
                    botStateMachine.halt();
        }
    }
}