package com.tradingbot.backend.trade.fsm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotStateMachineTest {

    @Test
    void initialStateIsStarting() {
        BotStateMachine fsm = new BotStateMachine();

        assertEquals(
                BotState.STARTING,
                fsm.getState()
        );
    }

    @Test
    void activateChangesStartingToActive() {
        BotStateMachine fsm = new BotStateMachine();

        boolean result = fsm.activate();

        assertTrue(result);

        assertEquals(
                BotState.ACTIVE,
                fsm.getState()
        );
    }

    @Test
    void haltedBotCannotBeActivatedAgain() {
        BotStateMachine fsm = new BotStateMachine();

        fsm.halt();

        boolean result = fsm.activate();

        assertFalse(result);

        assertEquals(
                BotState.HALTED,
                fsm.getState()
        );
    }
}