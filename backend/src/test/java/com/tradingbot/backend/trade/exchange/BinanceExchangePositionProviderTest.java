package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.TradingSymbolsProperties;
import com.tradingbot.backend.trade.exchange.dto.BinanceAccountResponse;
import com.tradingbot.backend.trade.exchange.dto.BinanceBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BinanceExchangePositionProviderTest {


    @Test
    void nullBalancesThrowsException() {

        BinanceAccountClient accountClient =
                mock(BinanceAccountClient.class);

        TradingSymbolsProperties symbols =
                new TradingSymbolsProperties(
                        List.of("ETHUSDT")
                );

        BinanceAccountResponse account =
                new BinanceAccountResponse(null);

        when(accountClient.getAccount())
                .thenReturn(account);

        BinanceExchangePositionProvider provider =
                new BinanceExchangePositionProvider(
                        accountClient,
                        symbols
                );

        assertThrows(
                IllegalStateException.class,
                provider::getPositions
        );
    }

    @Test
    void zeroBalanceIsNotIncluded() {

        BinanceAccountClient accountClient =
                mock(BinanceAccountClient.class);

        TradingSymbolsProperties symbols =
                new TradingSymbolsProperties(
                        List.of("ETHUSDT")
                );

        BinanceAccountResponse account =
                new BinanceAccountResponse(
                        List.of(
                                new BinanceBalance(
                                        "ETH",
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO
                                )
                        )
                );

        when(accountClient.getAccount())
                .thenReturn(account);

        BinanceExchangePositionProvider provider =
                new BinanceExchangePositionProvider(
                        accountClient,
                        symbols
                );

        Map<String, BigDecimal> result =
                provider.getPositions();

        assertTrue(result.isEmpty());
    }

    @Test
    void convertsManagedBalancesToPositions() {

        BinanceAccountClient accountClient =
                mock(BinanceAccountClient.class);

        TradingSymbolsProperties symbols =
                new TradingSymbolsProperties(
                        List.of(
                                "ETHUSDT",
                                "BTCUSDT"
                        )
                );

        BinanceAccountResponse account =
                new BinanceAccountResponse(
                        List.of(
                                new BinanceBalance(
                                        "ETH",
                                        new BigDecimal("0.02"),
                                        new BigDecimal("0.01")
                                ),
                                new BinanceBalance(
                                        "BTC",
                                        new BigDecimal("0.001"),
                                        BigDecimal.ZERO
                                ),
                                new BinanceBalance(
                                        "BNB",
                                        new BigDecimal("1.0"),
                                        BigDecimal.ZERO
                                )
                        )
                );

        when(accountClient.getAccount())
                .thenReturn(account);

        BinanceExchangePositionProvider provider =
                new BinanceExchangePositionProvider(
                        accountClient,
                        symbols
                );

        Map<String, BigDecimal> result =
                provider.getPositions();

        // 여기부터 직접 검증

        assertEquals(
                0,
                result.get("ETHUSDT")
                        .compareTo(new BigDecimal("0.03"))
        );

        assertEquals(
                0,
                result.get("BTCUSDT")
                        .compareTo(new BigDecimal("0.001"))
        );

        assertNull(
                result.get("BNBUSDT")
        );

        assertEquals(
                2,
                result.size()
        );

        verify(accountClient, times(1))
                .getAccount();

    }
}