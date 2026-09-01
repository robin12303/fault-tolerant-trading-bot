package com.tradingbot.backend.strategy.store;

import com.tradingbot.backend.strategy.dto.StrategySnapshot;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class StrategySnapshotStore {

    private final AtomicReference<StrategySnapshot> snapshot =
            new AtomicReference<>();

    public void set(StrategySnapshot newSnapshot) {
        snapshot.set(newSnapshot);
    }

    public StrategySnapshot get() {
        return snapshot.get();
    }
}