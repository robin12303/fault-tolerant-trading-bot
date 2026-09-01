package com.tradingbot.backend.trade.exchange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BinanceRequestSignerTest {

    @Test
    void signsPayloadWithHmacSha256() {

        BinanceRequestSigner signer =
                new BinanceRequestSigner();

        String payload =
                "timestamp=1770000000000&recvWindow=5000";

        String secret =
                "test-secret";

        String signature =
                signer.sign(payload, secret);

        assertEquals(
                "cf644640f843fa3b978c27e280b1bcc734e8aae7243db66ee754ef7bc808bea0",
                signature
        );
    }
}