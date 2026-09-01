package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.fsm.BotStateMachine;
import org.springframework.stereotype.Service;
import com.tradingbot.backend.trade.exchange.config.TradingSafetyProperties;


@Service
public class StartupReconciliationService {

    private final BotStateMachine botStateMachine;
    private final StartupReconciliationChecker checker;
    private final TradingSafetyProperties safetyProperties;


    public StartupReconciliationService(
            BotStateMachine botStateMachine,
            StartupReconciliationChecker checker,
            TradingSafetyProperties safetyProperties
    ) {
        this.botStateMachine = botStateMachine;
        this.checker = checker;
        this.safetyProperties = safetyProperties;
    }

    public void reconcile() {

        if (!safetyProperties.dedicatedAccount()) {
            botStateMachine.halt();
            return;
        }

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