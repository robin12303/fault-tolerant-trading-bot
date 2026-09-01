package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.exchange.dto.BinanceNewOrderResponse;
import com.tradingbot.backend.trade.order.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpServerErrorException;
import java.math.BigDecimal;
import java.time.Clock;

@Component
public class BinanceOrderClient implements OrderClient {

    private final RestClient restClient;
    private final BinanceApiProperties properties;
    private final BinanceRequestSigner signer;
    private final Clock clock;

    // 생성자를 직접 작성해보십시오.
    public BinanceOrderClient(
            RestClient.Builder builder,
            BinanceApiProperties properties,
            BinanceRequestSigner signer,
            Clock clock
    ){
        this.restClient = builder
                .baseUrl("https://api.binance.com")
                .build();
        this.properties = properties;
        this.signer = signer;
        this.clock = clock;

    }

    @Override
    public OrderSubmissionResult submit(OrderRequest request) {

        if (properties.key() == null
                || properties.key().isBlank()
                || properties.secret() == null
                || properties.secret().isBlank()) {

            throw new IllegalStateException(
                    "Binance API credentials are not configured"
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Order request must not be null"
            );
        }

        if (request.side() != OrderSide.BUY) {
            throw new IllegalArgumentException(
                    "Only MARKET BUY is supported"
            );
        }

        if (request.quoteQuantity() == null
                || request.quoteQuantity()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "quoteQuantity must be positive"
            );
        }

        if (request.clientOrderId() == null
                || request.clientOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "clientOrderId must not be blank"
            );
        }

        long timestamp = clock.millis();
        long recvWindow = 5000L;

        String payload =
                buildPayload(
                        request,
                        timestamp,
                        recvWindow
                );

        String signature =
                signer.sign(
                        payload,
                        properties.secret()
                );
        try {

            BinanceNewOrderResponse response =
                    restClient.post()
                            .uri(
                                    "/api/v3/order?"
                                            + payload
                                            + "&signature="
                                            + signature
                            )
                            .header(
                                    "X-MBX-APIKEY",
                                    properties.key()
                            )
                            .retrieve()
                            .body(BinanceNewOrderResponse.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Binance order response is null"
                );
            }

            return new OrderSubmissionResult(
                    OrderSubmissionStatus.ACCEPTED,
                    response.clientOrderId(),
                    response.orderId()
            );

        } catch (HttpClientErrorException.BadRequest e) {

            return new OrderSubmissionResult(
                    OrderSubmissionStatus.REJECTED,
                    request.clientOrderId(),
                    null
            );

        } catch (
                HttpServerErrorException
                | ResourceAccessException e
        ) {

            return new OrderSubmissionResult(
                    OrderSubmissionStatus.UNKNOWN,
                    request.clientOrderId(),
                    null
            );
        }catch (HttpClientErrorException.TooManyRequests e) {

            return new OrderSubmissionResult(
                    OrderSubmissionStatus.RATE_LIMITED,
                    request.clientOrderId(),
                    null
            );

        }
    }

    static String buildPayload(
            OrderRequest request,
            long timestamp,
            long recvWindow
    ) {

        return "symbol=" + request.symbol()
                + "&side=" + request.side().name()
                + "&type=MARKET"
                + "&quoteOrderQty=" + request.quoteQuantity().toPlainString()
                + "&newClientOrderId=" + request.clientOrderId()
                + "&newOrderRespType=ACK"
                + "&recvWindow=" + recvWindow
                + "&timestamp=" + timestamp;
    }


}