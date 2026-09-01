package com.tradingbot.backend.market.client;

import com.tradingbot.backend.market.dto.BinancePriceResponse;
import com.tradingbot.backend.market.dto.BinanceKline;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.tradingbot.backend.market.dto.BinanceExchangeInfoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class BinanceMarketClient {

    private final RestClient restClient;

    public BinanceMarketClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.binance.com")
                .build();
    }

    public BinancePriceResponse getPrice(String symbol) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/ticker/price")
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .body(BinancePriceResponse.class);
    }

    public BinanceExchangeInfoResponse getExchangeInfo(String symbol) {

        BinanceExchangeInfoResponse response =
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/v3/exchangeInfo")
                                .queryParam("symbol", symbol)
                                .build())
                        .retrieve()
                        .body(BinanceExchangeInfoResponse.class);

        if (response == null || response.symbols() == null) {
            throw new IllegalStateException(
                    "Binance exchange info response is invalid"
            );
        }

        return response;
    }

    public List<BinanceKline> getDailyKlines(String symbol, int limit) {

        List<List<Object>> raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/klines")
                        .queryParam("symbol", symbol)
                        .queryParam("interval", "1d")
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (raw == null) {
            throw new IllegalStateException("Binance kline response is null");
        }

        return raw.stream()
                .map(k -> new BinanceKline(
                        ((Number) k.get(0)).longValue(),
                        new BigDecimal(k.get(1).toString()),
                        new BigDecimal(k.get(2).toString()),
                        new BigDecimal(k.get(3).toString()),
                        new BigDecimal(k.get(4).toString()),
                        ((Number) k.get(6)).longValue()
                ))
                .toList();
    }

    public long getServerTime() {
        Map<String, Object> response = restClient.get()
                .uri("/api/v3/time")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.get("serverTime") == null) {
            throw new IllegalStateException("Binance server time response is invalid");
        }

        return ((Number) response.get("serverTime")).longValue();
    }
}