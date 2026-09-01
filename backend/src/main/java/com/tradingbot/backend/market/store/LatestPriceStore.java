package com.tradingbot.backend.market.store;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LatestPriceStore {

    private final ConcurrentHashMap<String, PriceSnapshot> prices =
            new ConcurrentHashMap<>();

    public void update(String symbol, BigDecimal price) {
        prices.put(
                symbol,
                new PriceSnapshot(price, Instant.now())
        );
    }

    public PriceSnapshot get(String symbol) {
        return prices.get(symbol);
    }

    public record PriceSnapshot(
            BigDecimal price,
            Instant receivedAt
    ) {}
}