package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.fsm.BotState;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("live")
@SpringBootTest
@Testcontainers
class StartupRecoveryLiveTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4");

    @Autowired
    BotStateMachine botStateMachine;

    @Test
    void activatesAfterRealStartupRecovery() {

        assertEquals(
                BotState.ACTIVE,
                botStateMachine.getState()
        );
    }
}