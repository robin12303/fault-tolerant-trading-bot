package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.order.ExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.ExchangeOrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.tradingbot.backend.trade.exchange.dto.BinanceOrderQueryResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class BinanceOrderQueryClientTest {

    @Test
    void buildsOrderQueryPayload() {

        String payload =
                BinanceOrderQueryClient.buildPayload(
                        "ETHUSDT",
                        "bot-123",
                        123456789L,
                        5000L
                );

        assertEquals(
                "symbol=ETHUSDT"
                        + "&origClientOrderId=bot-123"
                        + "&recvWindow=5000"
                        + "&timestamp=123456789",
                payload
        );
    }

    @Test
    void getsOrderByClientOrderId() {

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
        long recvWindow = 5000L;

        String payload =
                BinanceOrderQueryClient.buildPayload(
                        "ETHUSDT",
                        "bot-123",
                        timestamp,
                        recvWindow
                );

        String signature =
                signer.sign(
                        payload,
                        "test-secret"
                );

        server.expect(
                        requestTo(
                                "https://api.binance.com/api/v3/order?"
                                        + payload
                                        + "&signature="
                                        + signature
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
                                  "symbol": "ETHUSDT",
                                  "orderId": 12345,
                                  "clientOrderId": "bot-123",
                                  "status": "FILLED",
                                  "executedQty": "0.00650000"
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        BinanceOrderQueryClient client =
                new BinanceOrderQueryClient(
                        builder,
                        properties,
                        signer,
                        clock
                );

        ExchangeOrderSnapshot response =
                client.getOrder(
                        "ETHUSDT",
                        "bot-123"
                );

        assertEquals(
                "ETHUSDT",
                response.symbol()
        );

        assertEquals(
                12345L,
                response.exchangeOrderId()
        );

        assertEquals(
                "bot-123",
                response.clientOrderId()
        );

        assertEquals(
                ExchangeOrderStatus.FILLED,
                response.status()
        );

        assertEquals(
                0,
                response.executedQty()
                        .compareTo(
                                new BigDecimal("0.00650000")
                        )
        );

        server.verify();
    }

    @Test
    void convertsFilledResponseToSnapshot() {

        BinanceOrderQueryResponse response =
                new BinanceOrderQueryResponse(
                        "ETHUSDT",
                        12345L,
                        "bot-123",
                        "FILLED",
                        new BigDecimal("0.00650000")
                );

        ExchangeOrderSnapshot snapshot =
                BinanceOrderQueryClient.toSnapshot(response);

        assertEquals(
                "ETHUSDT",
                snapshot.symbol()
        );

        assertEquals(
                "bot-123",
                snapshot.clientOrderId()
        );

        assertEquals(
                12345L,
                snapshot.exchangeOrderId()
        );

        assertEquals(
                ExchangeOrderStatus.FILLED,
                snapshot.status()
        );

        assertEquals(
                0,
                snapshot.executedQty()
                        .compareTo(
                                new BigDecimal("0.00650000")
                        )
        );
    }

    @Test
    void unknownBinanceStatusBecomesUnknown() {

        BinanceOrderQueryResponse response =
                new BinanceOrderQueryResponse(
                        "ETHUSDT",
                        12345L,
                        "bot-123",
                        "SOME_NEW_STATUS",
                        BigDecimal.ZERO
                );

        ExchangeOrderSnapshot snapshot =
                BinanceOrderQueryClient.toSnapshot(response);

        assertEquals(
                ExchangeOrderStatus.UNKNOWN,
                snapshot.status()
        );
    }
}