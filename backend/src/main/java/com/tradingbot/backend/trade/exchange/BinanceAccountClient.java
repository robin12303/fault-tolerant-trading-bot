package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import com.tradingbot.backend.trade.exchange.dto.BinanceAccountResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BinanceAccountClient {

    private final RestClient restClient;
    private final BinanceApiProperties properties;
    private final BinanceRequestSigner signer;

    public BinanceAccountClient(
            RestClient.Builder builder,
            BinanceApiProperties properties,
            BinanceRequestSigner signer
    ) {
        this.restClient = builder
                .baseUrl("https://api.binance.com")
                .build();

        this.properties = properties;
        this.signer = signer;
    }

    public BinanceAccountResponse getAccount() {

        if (properties.key() == null
                || properties.key().isBlank()
                || properties.secret() == null
                || properties.secret().isBlank()) {

            throw new IllegalStateException(
                    "Binance API credentials are not configured"
            );
        }

        long timestamp = System.currentTimeMillis();
        long recvWindow = 5000;

        String payload =
                "timestamp=" + timestamp
                        + "&recvWindow=" + recvWindow;

        String signature =
                signer.sign(
                        payload,
                        properties.secret()
                );

        RestClient.RequestHeadersSpec<?> request =
                restClient.get()
                        .uri(
                                "/api/v3/account"
                                        + "?timestamp={timestamp}"
                                        + "&recvWindow={recvWindow}"
                                        + "&signature={signature}",
                                timestamp,
                                recvWindow,
                                signature
                        );

        request.header(
                "X-MBX-APIKEY",
                properties.key()
        );

        BinanceAccountResponse response =
                request.retrieve()
                        .body(BinanceAccountResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Binance account response is null"
            );
        }

        return response;
    }
}