package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.BinanceApiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

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
                        signer
                );

        assertThrows(
                IllegalStateException.class,
                client::getAccount
        );
    }
}