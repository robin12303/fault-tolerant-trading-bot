package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.exchange.dto.BinanceAccountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.Tag;

@Tag("live")
class BinanceAccountClientLiveTest {

    @Test
    void readsRealBinanceAccount() {

        String apiKey =
                System.getenv("BINANCE_API_KEY");

        String apiSecret =
                System.getenv("BINANCE_API_SECRET");

        assumeTrue(
                apiKey != null && !apiKey.isBlank(),
                "BINANCE_API_KEY is not configured"
        );

        assumeTrue(
                apiSecret != null && !apiSecret.isBlank(),
                "BINANCE_API_SECRET is not configured"
        );

        BinanceApiProperties properties =
                new BinanceApiProperties(
                        apiKey,
                        apiSecret
                );

        BinanceRequestSigner signer =
                new BinanceRequestSigner();

        BinanceAccountClient client =
                new BinanceAccountClient(
                        RestClient.builder(),
                        properties,
                        signer,
                        Clock.systemUTC()
                );

        BinanceAccountResponse response =
                client.getAccount();

        assertNotNull(response);
        assertNotNull(response.balances());
    }
}