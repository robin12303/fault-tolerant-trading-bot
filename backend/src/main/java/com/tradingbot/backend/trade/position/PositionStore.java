package com.tradingbot.backend.trade.position;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
@Component
public class PositionStore {

    private final ConcurrentHashMap<String, PositionSnapshot> positions =
            new ConcurrentHashMap<>();
    private final PositionRepository positionRepository;
    public PositionStore(
            PositionRepository positionRepository
    ) {
        // 여기 직접 작성
        this.positionRepository = positionRepository;
    }
    public void setPosition(
            String symbol,
            BigDecimal baseQty
    ) {
        PositionEntity entity = new PositionEntity(
                symbol,
                baseQty,
                // 현재 시간
                LocalDateTime.now()
        );

        positionRepository.save(entity);

        positions.put(
                symbol,
                new PositionSnapshot(
                        symbol,
                        baseQty
                )
        );
    }

    public PositionSnapshot getPosition(String symbol) {
        return positions.get(symbol);
    }

    public void clearPosition(String symbol) {
        positionRepository.deleteById(symbol);
        positions.remove(symbol);
    }

    public void loadFromDatabase() {

        positionRepository.findAll()
                .forEach(entity -> {
                    positions.put(
                            entity.getSymbol(),
                            new PositionSnapshot(
                                    entity.getSymbol(),
                                    entity.getBaseQty()
                            )
                    );
                });
    }

    public Map<String, PositionSnapshot> getAllPositions() {
        return Map.copyOf(positions);
    }

}