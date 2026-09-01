package com.tradingbot.backend.trade.exchange;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class BinanceRequestSigner {

    public String sign(
            String payload,
            String secretKey
    ) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec keySpec =
                    new SecretKeySpec(
                            secretKey.getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"
                    );

            mac.init(keySpec);

            byte[] hash =
                    mac.doFinal(
                            payload.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of()
                    .formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to sign Binance request",
                    e
            );
        }
    }
}