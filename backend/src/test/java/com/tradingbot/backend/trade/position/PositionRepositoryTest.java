package com.tradingbot.backend.trade.position;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class PositionRepositoryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4");

    @Autowired
    PositionRepository positionRepository;

    @Test
    void saveAndFindPosition() {

        PositionEntity positionEntity =
                new PositionEntity(
                        "ETHUSDT",
                        new BigDecimal("0.03"),
                        LocalDateTime.now()
                );

        positionRepository.save(positionEntity);

        Optional<PositionEntity> s =
                positionRepository.findById("ETHUSDT");

        assertTrue(s.isPresent());

        PositionEntity saved =
                s.get();

        assertEquals(
                "ETHUSDT",
                saved.getSymbol()
        );

        assertEquals(
                0,
                saved.getBaseQty()
                        .compareTo(new BigDecimal("0.03"))
        );
    }
}