package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.position.PositionSnapshot;
import com.tradingbot.backend.trade.position.PositionStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PositionStartupReconciliationCheckerTest {

    @Test
    void bothSidesEmptyAreConsistent() {

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangePositionProvider exchangePositionProvider =
                mock(ExchangePositionProvider.class);

        when(positionStore.getAllPositions())
                .thenReturn(Map.of());

        when(exchangePositionProvider.getPositions())
                .thenReturn(Map.of());

        PositionStartupReconciliationChecker checker =
                new PositionStartupReconciliationChecker(
                        positionStore,
                        exchangePositionProvider
                );

        StartupReconciliationResult result =
                checker.check();

        assertEquals(
                StartupReconciliationResult.CONSISTENT,
                result
        );
    }

    @Test
    void extraExchangePositionIsInconsistent() {

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangePositionProvider exchangePositionProvider =
                mock(ExchangePositionProvider.class);

        when(positionStore.getAllPositions())
                .thenReturn(Map.of());

        when(exchangePositionProvider.getPositions())
                .thenReturn(
                        Map.of(
                                "ETHUSDT",
                                new BigDecimal("0.03")
                        )
                );

        PositionStartupReconciliationChecker checker =
                new PositionStartupReconciliationChecker(
                        positionStore,
                        exchangePositionProvider
                );

        StartupReconciliationResult result =
                checker.check();

        assertEquals(
                StartupReconciliationResult.INCONSISTENT,
                result
        );
    }

    @Test
    void exchangeFailureIsUnavailable() {

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangePositionProvider exchangePositionProvider =
                mock(ExchangePositionProvider.class);

        when(positionStore.getAllPositions())
                .thenReturn(Map.of());

        when(exchangePositionProvider.getPositions())
                .thenThrow(
                        new RuntimeException("Exchange unavailable")
                );

        PositionStartupReconciliationChecker checker =
                new PositionStartupReconciliationChecker(
                        positionStore,
                        exchangePositionProvider
                );

        StartupReconciliationResult result =
                checker.check();

        assertEquals(
                StartupReconciliationResult.UNAVAILABLE,
                result
        );
    }

    @Test
    void differentSymbolIsInconsistent() {

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangePositionProvider exchangePositionProvider =
                mock(ExchangePositionProvider.class);

        when(positionStore.getAllPositions())
                .thenReturn(
                        Map.of(
                                "ETHUSDT",
                                new PositionSnapshot(
                                        "ETHUSDT",
                                        new BigDecimal("0.03")
                                )
                        )
                );

        when(exchangePositionProvider.getPositions())
                .thenReturn(
                        Map.of(
                                "BTCUSDT",
                                new BigDecimal("0.03")
                        )
                );

        PositionStartupReconciliationChecker checker =
                new PositionStartupReconciliationChecker(
                        positionStore,
                        exchangePositionProvider
                );

        StartupReconciliationResult result =
                checker.check();

        assertEquals(
                StartupReconciliationResult.INCONSISTENT,
                result
        );
    }

    @Test
    void differentQuantityIsInconsistent() {

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangePositionProvider exchangePositionProvider =
                mock(ExchangePositionProvider.class);

        when(positionStore.getAllPositions())
                .thenReturn(
                        Map.of(
                                "ETHUSDT",
                                new PositionSnapshot(
                                        "ETHUSDT",
                                        new BigDecimal("0.03")
                                )
                        )
                );

        when(exchangePositionProvider.getPositions())
                .thenReturn(
                        Map.of(
                                "ETHUSDT",
                                new BigDecimal("0.04")
                        )
                );

        PositionStartupReconciliationChecker checker =
                new PositionStartupReconciliationChecker(
                        positionStore,
                        exchangePositionProvider
                );

        StartupReconciliationResult result =
                checker.check();

        assertEquals(
                StartupReconciliationResult.INCONSISTENT,
                result
        );
    }

    @Test
    void matchingPositionsAreConsistent() {

        PositionStore positionStore =
                mock(PositionStore.class);

        ExchangePositionProvider exchangePositionProvider =
                mock(ExchangePositionProvider.class);

        when(positionStore.getAllPositions())
                .thenReturn(
                        Map.of(
                                "ETHUSDT",
                                new PositionSnapshot(
                                        "ETHUSDT",
                                        new BigDecimal("0.03")
                                )
                        )
                );

        when(exchangePositionProvider.getPositions())
                .thenReturn(
                        Map.of(
                                "ETHUSDT",
                                new BigDecimal("0.03000000")
                        )
                );

        PositionStartupReconciliationChecker checker =
                new PositionStartupReconciliationChecker(
                        positionStore,
                        exchangePositionProvider
                );

        StartupReconciliationResult result =
                checker.check();

        assertEquals(
                StartupReconciliationResult.CONSISTENT,
                result
        );
    }
}