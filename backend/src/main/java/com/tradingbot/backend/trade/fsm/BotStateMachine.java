package com.tradingbot.backend.trade.fsm;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class BotStateMachine {

    private final AtomicReference<BotState> state =
            new AtomicReference<>(BotState.STARTING);

    public BotState getState() {
        return state.get();
    }

    public boolean activate() {
        // STARTING -> ACTIVE
        // 직접 작성

        return state.compareAndSet(
                BotState.STARTING,
                BotState.ACTIVE
        );
    }

    public void halt() {
        // 어떤 상태든 HALTED로 변경
        state.set(BotState.HALTED);
    }

    public boolean isActive() {
        // 현재 ACTIVE인지 반환
        return state.get() == BotState.ACTIVE;
    }
}