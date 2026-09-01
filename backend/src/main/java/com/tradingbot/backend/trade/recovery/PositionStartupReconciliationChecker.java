package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.position.PositionSnapshot;
import com.tradingbot.backend.trade.position.PositionStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PositionStartupReconciliationChecker
        implements StartupReconciliationChecker {

    private final PositionStore positionStore;
    private final ExchangePositionProvider exchangePositionProvider;

    public PositionStartupReconciliationChecker(
            PositionStore positionStore,
            ExchangePositionProvider exchangePositionProvider
    ) {
        this.positionStore = positionStore;
        this.exchangePositionProvider = exchangePositionProvider;
    }

    @Override
    public StartupReconciliationResult check() {

        try {
            Map<String, PositionSnapshot> localPositions =
                    positionStore.getAllPositions();

            Map<String, BigDecimal> exchangePositions =
                    exchangePositionProvider.getPositions();

            if (localPositions.size() != exchangePositions.size()) {
                return StartupReconciliationResult.INCONSISTENT;
            }

            for (Map.Entry<String, PositionSnapshot> entry
                    : localPositions.entrySet()) {

                String symbol = entry.getKey();
                PositionSnapshot localPosition = entry.getValue();

                BigDecimal exchangeQty =
                        exchangePositions.get(symbol);

                if (exchangeQty == null) {
                    return StartupReconciliationResult.INCONSISTENT;
                }

                if (localPosition.baseQty()
                        .compareTo(exchangeQty) != 0) {

                    return StartupReconciliationResult.INCONSISTENT;
                }
            }

            return StartupReconciliationResult.CONSISTENT;

        } catch (RuntimeException e) {
            return StartupReconciliationResult.UNAVAILABLE;
        }
    }
}