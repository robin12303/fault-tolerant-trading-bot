package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.exchange.config.TradingSafetyProperties;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import org.springframework.stereotype.Service;

@Service
public class StartupReconciliationService {

    private final BotStateMachine botStateMachine;
    private final OrderRecoveryService orderRecoveryService;
    private final StartupReconciliationChecker checker;
    private final TradingSafetyProperties safetyProperties;

    public StartupReconciliationService(
            BotStateMachine botStateMachine,
            OrderRecoveryService orderRecoveryService,
            StartupReconciliationChecker checker,
            TradingSafetyProperties safetyProperties
    ) {
        this.botStateMachine =
                botStateMachine;

        this.orderRecoveryService =
                orderRecoveryService;

        this.checker =
                checker;

        this.safetyProperties =
                safetyProperties;
    }

    public void reconcile() {

        // 1. 전용 계정이 아니면 무조건 거래 중단
        if (!safetyProperties.dedicatedAccount()) {

            botStateMachine.halt();
            return;
        }

        // 2. 과거 미확정 주문부터 복구
        StartupReconciliationResult orderResult =
                orderRecoveryService.recover();

        if (orderResult
                != StartupReconciliationResult.CONSISTENT) {

            botStateMachine.halt();
            return;
        }

        // 3. 주문 문제가 없을 때만
        // 실제 계좌 포지션과 로컬 포지션 비교
        StartupReconciliationResult positionResult =
                checker.check();

        switch (positionResult) {

            case CONSISTENT ->
                    botStateMachine.activate();

            case INCONSISTENT,
                 UNAVAILABLE ->
                    botStateMachine.halt();
        }
    }
}