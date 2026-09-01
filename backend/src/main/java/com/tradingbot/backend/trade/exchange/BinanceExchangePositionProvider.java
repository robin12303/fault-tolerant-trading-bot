package com.tradingbot.backend.trade.exchange;

import com.tradingbot.backend.trade.exchange.config.TradingSymbolsProperties;
import com.tradingbot.backend.trade.exchange.dto.BinanceAccountResponse;
import com.tradingbot.backend.trade.recovery.ExchangePositionProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class BinanceExchangePositionProvider
        implements ExchangePositionProvider {

    private final BinanceAccountClient accountClient;
    private final TradingSymbolsProperties tradingSymbolsProperties;

    public BinanceExchangePositionProvider(
            BinanceAccountClient accountClient,
            TradingSymbolsProperties tradingSymbolsProperties
    ) {
        this.accountClient = accountClient;
        this.tradingSymbolsProperties = tradingSymbolsProperties;
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

            if (!symbol.endsWith("USDT")) {
                throw new IllegalStateException(
                        "Unsupported trading symbol: " + symbol
                );
            }

            String baseAsset =
                    symbol.substring(
                            0,
                            symbol.length() - "USDT".length()
                    );

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