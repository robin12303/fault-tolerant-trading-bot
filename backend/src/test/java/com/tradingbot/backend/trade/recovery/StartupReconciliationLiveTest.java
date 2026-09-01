package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.market.client.BinanceMarketClient;
import com.tradingbot.backend.trade.exchange.BinanceAccountClient;
import com.tradingbot.backend.trade.exchange.BinanceExchangePositionProvider;
import com.tradingbot.backend.trade.exchange.BinanceRequestSigner;
import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.exchange.config.TradingSafetyProperties;
import com.tradingbot.backend.trade.exchange.config.TradingSymbolsProperties;
import com.tradingbot.backend.trade.fsm.BotState;
import com.tradingbot.backend.trade.fsm.BotStateMachine;
import com.tradingbot.backend.trade.position.PositionStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("live")
class StartupReconciliationLiveTest {

    @Test
    void activatesWithFreshDedicatedAccount() {

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

        BinanceAccountClient accountClient =
                new BinanceAccountClient(
                        RestClient.builder(),
                        new BinanceApiProperties(
                                apiKey,
                                apiSecret
                        ),
                        new BinanceRequestSigner(),
                        Clock.systemUTC()
                );

        BinanceMarketClient marketClient =
                new BinanceMarketClient(
                        RestClient.builder()
                );

        BinanceExchangePositionProvider exchangeProvider =
                new BinanceExchangePositionProvider(
                        accountClient,
                        new TradingSymbolsProperties(
                                List.of(
                                        "ETHUSDT",
                                        "BTCUSDT"
                                )
                        ),
                        marketClient
                );

        PositionStore positionStore =
                mock(PositionStore.class);

        when(positionStore.getAllPositions())
                .thenReturn(Map.of());

        PositionStartupReconciliationChecker checker =
                new PositionStartupReconciliationChecker(
                        positionStore,
                        exchangeProvider
                );

        BotStateMachine stateMachine =
                new BotStateMachine();

        StartupReconciliationService service =
                new StartupReconciliationService(
                        stateMachine,
                        checker,
                        new TradingSafetyProperties(true)
                );

        service.reconcile();

        assertEquals(
                BotState.ACTIVE,
                stateMachine.getState()
        );
    }
}