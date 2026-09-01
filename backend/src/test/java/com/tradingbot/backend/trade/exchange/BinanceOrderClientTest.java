package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.order.OrderRequest;
import com.tradingbot.backend.trade.order.OrderSide;
import com.tradingbot.backend.trade.order.OrderSubmissionResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import com.tradingbot.backend.trade.exchange.dto.BinanceNewOrderResponse;
import com.tradingbot.backend.trade.order.OrderSubmissionStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class BinanceOrderClientTest {
    @Test
    void submitsSignedMarketBuyOrder() {

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

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.50"),
                        "bot-123"
                );

        String payload =
                BinanceOrderClient.buildPayload(
                        request,
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
                .andExpect(method(HttpMethod.POST))
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
                                  "orderId": 12345,
                                  "clientOrderId": "bot-123"
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        BinanceOrderClient client =
                new BinanceOrderClient(
                        builder,
                        properties,
                        signer,
                        clock
                );

        OrderSubmissionResult result =
                client.submit(request);

        assertEquals(
                OrderSubmissionStatus.ACCEPTED,
                result.status()
        );

        assertEquals(
                "bot-123",
                result.clientOrderId()
        );

        assertEquals(
                12345L,
                result.exchangeOrderId()
        );

        server.verify();
    }
    @Test
    void buildsMarketBuyPayload() {

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.50"),
                        "bot-123"
                );

        long timestamp = 123456789L;
        long recvWindow = 5000L;

        String payload =
                BinanceOrderClient.buildPayload(
                        request,
                        timestamp,
                        recvWindow
                );

        // 여기 assertEquals를 직접 작성
        assertEquals(
                "symbol=ETHUSDT&side=BUY&type=MARKET&quoteOrderQty=20.50&newClientOrderId=bot-123&newOrderRespType=ACK&recvWindow=5000&timestamp=123456789",
                payload
        );
    }

    @Test
    void serverErrorReturnsUnknown() {

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

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.50"),
                        "bot-123"
                );

        server.expect(
                        requestTo(
                                org.hamcrest.Matchers.containsString(
                                        "/api/v3/order?"
                                )
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        BinanceOrderClient client =
                new BinanceOrderClient(
                        builder,
                        properties,
                        signer,
                        clock
                );

        OrderSubmissionResult result =
                client.submit(request);

        assertEquals(
                OrderSubmissionStatus.UNKNOWN,
                result.status()
        );

        assertEquals(
                "bot-123",
                result.clientOrderId()
        );

        assertEquals(
                null,
                result.exchangeOrderId()
        );

        server.verify();
    }

    @Test
    void networkFailureReturnsUnknown() {

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

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.50"),
                        "bot-123"
                );

        server.expect(
                requestTo(
                        org.hamcrest.Matchers.containsString(
                                "/api/v3/order"
                        )
                )
        ).andRespond(
                withException(
                        new java.io.IOException(
                                "simulated network failure"
                        )
                )
        );

        BinanceOrderClient client =
                new BinanceOrderClient(
                        builder,
                        properties,
                        signer,
                        clock
                );

        OrderSubmissionResult result =
                client.submit(request);

        assertEquals(
                OrderSubmissionStatus.UNKNOWN,
                result.status()
        );

        assertEquals(
                "bot-123",
                result.clientOrderId()
        );

        assertEquals(
                null,
                result.exchangeOrderId()
        );

        server.verify();
    }
    @Test
    void badRequestReturnsRejected() {

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

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.50"),
                        "bot-123"
                );

        server.expect(
                        requestTo(
                                org.hamcrest.Matchers.containsString(
                                        "/api/v3/order"
                                )
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("""
                            {
                              "code": -1013,
                              "msg": "Filter failure"
                            }
                            """)
                );

        BinanceOrderClient client =
                new BinanceOrderClient(
                        builder,
                        properties,
                        signer,
                        clock
                );

        OrderSubmissionResult result =
                client.submit(request);

        assertEquals(
                OrderSubmissionStatus.REJECTED,
                result.status()
        );

        assertEquals(
                "bot-123",
                result.clientOrderId()
        );

        assertEquals(
                null,
                result.exchangeOrderId()
        );

        server.verify();
    }

    @Test
    void tooManyRequestsReturnsRateLimited() {

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

        OrderRequest request =
                new OrderRequest(
                        "ETHUSDT",
                        OrderSide.BUY,
                        new BigDecimal("20.50"),
                        "bot-123"
                );

        server.expect(
                        requestTo(
                                org.hamcrest.Matchers.containsString(
                                        "/api/v3/order"
                                )
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.TOO_MANY_REQUESTS)
                );

        BinanceOrderClient client =
                new BinanceOrderClient(
                        builder,
                        properties,
                        signer,
                        clock
                );

        OrderSubmissionResult result =
                client.submit(request);

        assertEquals(
                OrderSubmissionStatus.RATE_LIMITED,
                result.status()
        );

        assertEquals(
                "bot-123",
                result.clientOrderId()
        );

        assertEquals(
                null,
                result.exchangeOrderId()
        );

        server.verify();
    }
}