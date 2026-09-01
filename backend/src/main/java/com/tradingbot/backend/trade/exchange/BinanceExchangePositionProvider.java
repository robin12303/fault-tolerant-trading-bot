package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.TradingSymbolsProperties;
import com.tradingbot.backend.trade.exchange.dto.BinanceAccountResponse;
import com.tradingbot.backend.trade.recovery.ExchangePositionProvider;
import org.springframework.stereotype.Component;
import com.tradingbot.backend.market.client.BinanceMarketClient;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.tradingbot.backend.market.dto.BinanceExchangeInfoResponse;
import com.tradingbot.backend.market.dto.BinanceSymbolInfo;
@Component
public class BinanceExchangePositionProvider
        implements ExchangePositionProvider {

    private final BinanceAccountClient accountClient;
    private final TradingSymbolsProperties tradingSymbolsProperties;
    private final BinanceMarketClient marketClient;

    public BinanceExchangePositionProvider(
            BinanceAccountClient accountClient,
            TradingSymbolsProperties tradingSymbolsProperties,
            BinanceMarketClient marketClient
    ) {
        this.accountClient = accountClient;
        this.tradingSymbolsProperties = tradingSymbolsProperties;
        this.marketClient = marketClient;
    }

    @Override
    public Map<String, BigDecimal> getPositions() {

        BinanceAccountResponse account =
                accountClient.getAccount();

        if (account.balances() == null) {
            throw new IllegalStateException(
                    "Binance balances are null"
            );
        }

        Map<String, BigDecimal> result =
                new HashMap<>();

        for (String symbol : tradingSymbolsProperties.symbols()) {
            BinanceExchangeInfoResponse exchangeInfo =
                    marketClient.getExchangeInfo(symbol);

            BinanceSymbolInfo symbolInfo =
                    exchangeInfo.symbols().stream()
                            .filter(info ->
                                    symbol.equals(info.symbol())
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalStateException(
                                            "Symbol not found in exchange info: "
                                                    + symbol
                                    )
                            );

            String baseAsset =
                    symbolInfo.baseAsset();

            if (baseAsset == null || baseAsset.isBlank()) {
                throw new IllegalStateException(
                        "Base asset is missing for symbol: "
                                + symbol
                );
            }


            account.balances().stream()
                    .filter(balance ->
                            balance.asset().equals(baseAsset)
                    )
                    .findFirst()
                    .ifPresent(balance -> {

                        BigDecimal total =
                                balance.free()
                                        .add(balance.locked());

                        if (total.compareTo(BigDecimal.ZERO) > 0) {
                            result.put(symbol, total);
                        }
                    });
        }

        return Map.copyOf(result);
    }
}