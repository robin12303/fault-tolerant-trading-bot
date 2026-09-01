package com.tradingbot.backend.market.controller;

import com.tradingbot.backend.market.client.BinanceMarketClient;
import com.tradingbot.backend.market.dto.BinancePriceResponse;
import com.tradingbot.backend.market.dto.BinanceKline;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tradingbot.backend.market.dto.BinanceExchangeInfoResponse;
import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final BinanceMarketClient binanceMarketClient;

    public MarketController(BinanceMarketClient binanceMarketClient) {
        this.binanceMarketClient = binanceMarketClient;
    }

    @GetMapping("/price")
    public BinancePriceResponse getPrice(
            @RequestParam(defaultValue = "ETHUSDT") String symbol
    ) {
        return binanceMarketClient.getPrice(symbol);
    }

    @GetMapping("/exchange-info")
    public BinanceExchangeInfoResponse getExchangeInfo(
            @RequestParam(defaultValue = "ETHUSDT") String symbol
    ) {
        return binanceMarketClient.getExchangeInfo(symbol);
    }

    @GetMapping("/klines")
    public List<BinanceKline> getKlines(
            @RequestParam(defaultValue = "ETHUSDT") String symbol
    ) {
        return binanceMarketClient.getDailyKlines(symbol, 6);
    }
}