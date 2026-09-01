package com.tradingbot.backend.trade.position;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PositionStoreTest {

    @Test
    void getAllPositionsReturnsCurrentPositions() {

        PositionRepository repository =
                mock(PositionRepository.class);

        PositionStore store =
                new PositionStore(repository);

        store.setPosition(
                "ETHUSDT",
                new BigDecimal("0.03")
        );

        store.setPosition(
                "BTCUSDT",
                new BigDecimal("0.001")
        );

        Map<String, PositionSnapshot> positions =
                store.getAllPositions();

        assertEquals(2, positions.size());

        assertNotNull(positions.get("ETHUSDT"));
        assertNotNull(positions.get("BTCUSDT"));
    }

    @Test
    void loadFromDatabaseRestoresPositionsToMemory() {

        PositionRepository repository =
                mock(PositionRepository.class);

        PositionStore store =
                new PositionStore(repository);

        PositionEntity eth = new PositionEntity(
                "ETHUSDT",
                new BigDecimal("0.03"),
                LocalDateTime.now()
        );

        PositionEntity btc = new PositionEntity(
                "BTCUSDT",
                new BigDecimal("0.001"),
                LocalDateTime.now()
        );

        when(repository.findAll())
                .thenReturn(List.of(eth, btc));

        store.loadFromDatabase();

        PositionSnapshot ethSnapshot =
                store.getPosition("ETHUSDT");

        assertNotNull(ethSnapshot);

        assertEquals(
                0,
                ethSnapshot.baseQty()
                        .compareTo(new BigDecimal("0.03"))
        );

        PositionSnapshot btcSnapshot =
                store.getPosition("BTCUSDT");

        assertNotNull(btcSnapshot);

        assertEquals(
                0,
                btcSnapshot.baseQty()
                        .compareTo(new BigDecimal("0.001"))
        );

        verify(repository, times(1))
                .findAll();
    }

    @Test
    void clearPositionKeepsMemoryWhenRepositoryDeleteFails() {

        PositionRepository repository =
                mock(PositionRepository.class);

        PositionStore store =
                new PositionStore(repository);

        store.setPosition(
                "ETHUSDT",
                new BigDecimal("0.03")
        );

        doThrow(new RuntimeException("DB delete failed"))
                .when(repository)
                .deleteById("ETHUSDT");

        assertThrows(
                RuntimeException.class,
                () -> store.clearPosition("ETHUSDT")
        );

        assertNotNull(
                store.getPosition("ETHUSDT")
        );
    }
    @Test
    void clearPositionDeletesFromRepositoryAndMemory(){
        PositionRepository repository =
                mock(PositionRepository.class);

        PositionStore store =
                new PositionStore(repository);

        store.setPosition(
                "ETHUSDT",
                new BigDecimal("0.03")
        );

        store.clearPosition("ETHUSDT");

        verify(repository, times(1))
                .deleteById("ETHUSDT");

        assertNull(
                store.getPosition("ETHUSDT")
        );
    }
    @Test
    void setPositionSavesToRepositoryAndMemory() {

        PositionRepository repository =
                mock(PositionRepository.class);

        PositionStore store =
                new PositionStore(repository);

        // 여기부터 직접 작성
        store.setPosition("ETHUSDT", new BigDecimal("0.03"));
        verify(repository, times(1))
                .save(any(PositionEntity.class));

        PositionSnapshot snapshot =
                store.getPosition("ETHUSDT");

        assertNotNull(snapshot);

        assertEquals(
                0,
                snapshot.baseQty()
                        .compareTo(new BigDecimal("0.03"))
        );
    }

    @Test
    void setPositionDoesNotUpdateMemoryWhenRepositorySaveFails() {

        PositionRepository repository =
                mock(PositionRepository.class);

        PositionStore store =
                new PositionStore(repository);

        // repository.save(...)가 예외를 던지게 설정
        when(repository.save(any(PositionEntity.class)))
                .thenThrow(new RuntimeException("DB save failed"));
        // store.setPosition(...) 호출 시 예외 발생 검증
        assertThrows(
                RuntimeException.class,
                () -> store.setPosition(
                        "ETHUSDT",
                        new BigDecimal("0.03")
                )
        );
        // store.getPosition("ETHUSDT") == null 검증
        assertNull(
                store.getPosition("ETHUSDT")
        );
    }
}