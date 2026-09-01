package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.tradingbot.backend.trade.exchange.dto.BinanceAccountResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinanceAccountClientTest {

    @Test
    void missingCredentialsRejectAccountRequest() {

        BinanceApiProperties properties =
                new BinanceApiProperties("", "");

        BinanceRequestSigner signer =
                new BinanceRequestSigner();

        BinanceAccountClient client =
                new BinanceAccountClient(
                        RestClient.builder(),
                        properties,
                        signer,
                        Clock.systemUTC()
                );

        assertThrows(
                IllegalStateException.class,
                client::getAccount
        );
    }

    @Test
    void sendsSignedAccountRequest() {

        RestClient.Builder builder =
                RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        BinanceApiProperties properties =
                new BinanceApiProperties(
                        "test-key",
                        "test-secret"
                );

        BinanceRequestSigner signer =
                new BinanceRequestSigner();

        Clock clock =
                Clock.fixed(
                        Instant.parse("2026-09-01T09:00:00Z"),
                        ZoneOffset.UTC
                );

        long timestamp = clock.millis();

        String payload =
                "timestamp=" + timestamp
                        + "&recvWindow=5000";

        String signature =
                signer.sign(
                        payload,
                        "test-secret"
                );

        server.expect(
                        requestTo(
                                "https://api.binance.com/api/v3/account"
                                        + "?timestamp=" + timestamp
                                        + "&recvWindow=5000"
                                        + "&signature=" + signature
                        )
                )
                .andExpect(method(HttpMethod.GET))
                .andExpect(
                        header(
                                "X-MBX-APIKEY",
                                "test-key"
                        )
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "balances": [
                                    {
                                      "asset": "ETH",
                                      "free": "0.02000000",
                                      "locked": "0.01000000"
                                    },
                                    {
                                      "asset": "BTC",
                                      "free": "0.00100000",
                                      "locked": "0.00000000"
                                    }
                                  ]
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        BinanceAccountClient client =
                new BinanceAccountClient(
                        builder,
                        properties,
                        signer,
                        clock
                );

        BinanceAccountResponse response =
                client.getAccount();

        assertEquals(
                2,
                response.balances().size()
        );

        assertEquals(
                "ETH",
                response.balances().get(0).asset()
        );

        assertEquals(
                0,
                response.balances()
                        .get(0)
                        .free()
                        .compareTo(
                                new BigDecimal("0.02000000")
                        )
        );

        server.verify();
    }
}