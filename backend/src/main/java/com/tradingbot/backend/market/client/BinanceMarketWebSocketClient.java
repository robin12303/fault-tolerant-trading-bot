package com.tradingbot.backend.market.client;

import com.tradingbot.backend.market.store.LatestPriceStore;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

@Component
public class BinanceMarketWebSocketClient {

    private static final String URL =
            "wss://stream.binance.com:9443/stream"
                    + "?streams=ethusdt@aggTrade/btcusdt@aggTrade";

    private final JsonMapper jsonMapper;
    private final LatestPriceStore latestPriceStore;

    public BinanceMarketWebSocketClient(
            JsonMapper jsonMapper,
            LatestPriceStore latestPriceStore
    ) {
        this.jsonMapper = jsonMapper;
        this.latestPriceStore = latestPriceStore;
    }

    @PostConstruct
    public void connect() {

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                        URI.create(URL),
                        new WebSocket.Listener() {

                            @Override
                            public void onOpen(WebSocket webSocket) {
                                System.out.println(
                                        "Binance WebSocket CONNECTED"
                                );

                                webSocket.request(1);
                            }

                            @Override
                            public CompletionStage<?> onText(
                                    WebSocket webSocket,
                                    CharSequence data,
                                    boolean last
                            ) {

                                try {

                                    JsonNode root = jsonMapper.readTree(data.toString());

                                    JsonNode event = root.get("data");

                                    String symbol = event.get("s").asText();

                                    BigDecimal price =
                                            new BigDecimal(event.get("p").asText());

                                    latestPriceStore.update(symbol, price);

                                } catch (Exception e) {

                                    System.err.println(
                                            "WebSocket parse error: "
                                                    + e.getMessage()
                                    );
                                }

                                webSocket.request(1);

                                return null;
                            }

                            @Override
                            public void onError(
                                    WebSocket webSocket,
                                    Throwable error
                            ) {
                                System.err.println(
                                        "Binance WebSocket ERROR: "
                                                + error.getMessage()
                                );
                            }
                        }
                );
    }
}