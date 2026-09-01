package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.market.client.BinanceMarketClient;
import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.exchange.config.TradingSymbolsProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("live")
class BinanceExchangePositionProviderLiveTest {

    @Test
    void readsRealExchangePositions() {

        String apiKey =
                System.getenv("BINANCE_API_KEY");

        String apiSecret =
                System.getenv("BINANCE_API_SECRET");

        assumeTrue(
                apiKey != null && !apiKey.isBlank()
        );

        assumeTrue(
                apiSecret != null && !apiSecret.isBlank()
        );

        BinanceApiProperties apiProperties =
                new BinanceApiProperties(
                        apiKey,
                        apiSecret
                );

        BinanceAccountClient accountClient =
                new BinanceAccountClient(
                        RestClient.builder(),
                        apiProperties,
                        new BinanceRequestSigner(),
                        Clock.systemUTC()
                );

        BinanceMarketClient marketClient =
                new BinanceMarketClient(
                        RestClient.builder()
                );

        TradingSymbolsProperties symbols =
                new TradingSymbolsProperties(
                        List.of(
                                "ETHUSDT",
                                "BTCUSDT"
                        )
                );

        BinanceExchangePositionProvider provider =
                new BinanceExchangePositionProvider(
                        accountClient,
                        symbols,
                        marketClient
                );

        Map<String, BigDecimal> positions =
                provider.getPositions();

        assertNotNull(positions);
    }
}