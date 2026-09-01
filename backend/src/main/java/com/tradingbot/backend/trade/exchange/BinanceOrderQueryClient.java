package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.exchange.dto.BinanceOrderQueryResponse;
import com.tradingbot.backend.trade.order.OrderQueryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.tradingbot.backend.trade.order.ExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.ExchangeOrderStatus;
import java.time.Clock;
import com.tradingbot.backend.trade.exchange.dto.BinanceErrorResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;


import java.math.BigDecimal;
@Component
public class BinanceOrderQueryClient
        implements OrderQueryClient {

    private final RestClient restClient;
    private final BinanceApiProperties properties;
    private final BinanceRequestSigner signer;
    private final Clock clock;

    public BinanceOrderQueryClient(
            RestClient.Builder builder,
            BinanceApiProperties properties,
            BinanceRequestSigner signer,
            Clock clock
    ) {
        this.restClient = builder
                .baseUrl("https://api.binance.com")
                .build();

        this.properties = properties;
        this.signer = signer;
        this.clock = clock;
    }

    @Override
    public ExchangeOrderSnapshot  getOrder(
            String symbol,
            String clientOrderId
    ) {

        if (properties.key() == null
                || properties.key().isBlank()
                || properties.secret() == null
                || properties.secret().isBlank()) {

            throw new IllegalStateException(
                    "Binance API credentials are not configured"
            );
        }

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "symbol must not be blank"
            );
        }

        if (clientOrderId == null || clientOrderId.isBlank()) {
            throw new IllegalArgumentException(
                    "clientOrderId must not be blank"
            );
        }

        long timestamp = clock.millis();
        long recvWindow = 5000L;

        String payload =
                buildPayload(
                        symbol,
                        clientOrderId,
                        timestamp,
                        recvWindow
                );

        String signature =
                signer.sign(
                        payload,
                        properties.secret()
                );

        try {

            RestClient.RequestHeadersSpec<?> httpRequest =
                    restClient.get()
                            .uri(
                                    "/api/v3/order?"
                                            + payload
                                            + "&signature="
                                            + signature
                            );

            httpRequest.header(
                    "X-MBX-APIKEY",
                    properties.key()
            );

            BinanceOrderQueryResponse response =
                    httpRequest.retrieve()
                            .body(BinanceOrderQueryResponse.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Binance order query response is null"
                );
            }

            return toSnapshot(response);

        } catch (HttpClientErrorException.BadRequest e) {

            BinanceErrorResponse error =
                    e.getResponseBodyAs(
                            BinanceErrorResponse.class
                    );

            if (error != null
                    && error.code() == -2013) {

                return new ExchangeOrderSnapshot(
                        symbol,
                        clientOrderId,
                        null,
                        ExchangeOrderStatus.NOT_FOUND,
                        BigDecimal.ZERO
                );
            }

            throw e;
        } catch (
                HttpServerErrorException
                | ResourceAccessException e
        ) {

            return new ExchangeOrderSnapshot(
                    symbol,
                    clientOrderId,
                    null,
                    ExchangeOrderStatus.UNKNOWN,
                    BigDecimal.ZERO
            );
        }
    }

    static String buildPayload(
            String symbol,
            String clientOrderId,
            long timestamp,
            long recvWindow
    ) {
        return "symbol=" + symbol
                + "&origClientOrderId=" + clientOrderId
                + "&recvWindow=" + recvWindow
                + "&timestamp=" + timestamp;
    }

    static ExchangeOrderSnapshot toSnapshot(
            BinanceOrderQueryResponse response
    ) {

        ExchangeOrderStatus status =
                switch (response.status()) {
                    case "NEW" ->
                            ExchangeOrderStatus.NEW;

                    case "PARTIALLY_FILLED" ->
                            ExchangeOrderStatus.PARTIALLY_FILLED;

                    case "FILLED" ->
                            ExchangeOrderStatus.FILLED;

                    case "CANCELED" ->
                            ExchangeOrderStatus.CANCELED;

                    case "REJECTED" ->
                            ExchangeOrderStatus.REJECTED;

                    case "EXPIRED" ->
                            ExchangeOrderStatus.EXPIRED;

                    case "EXPIRED_IN_MATCH" ->
                            ExchangeOrderStatus.EXPIRED_IN_MATCH;

                    default ->
                            ExchangeOrderStatus.UNKNOWN;
                };

        return new ExchangeOrderSnapshot(
                response.symbol(),
                response.clientOrderId(),
                response.orderId(),
                status,
                response.executedQty()
        );
    }


}