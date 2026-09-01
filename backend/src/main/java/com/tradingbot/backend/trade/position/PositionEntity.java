package com.tradingbot.backend.trade.position;


import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "positions")
public class PositionEntity {

    @Id
    private String symbol;

    @Column(name = "base_qty", nullable = false)
    private BigDecimal baseQty;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // JPA용 기본 생성자
    protected PositionEntity() {
    }

    // 우리가 직접 객체 만들 때 사용할 생성자
    public PositionEntity(
            String symbol,
            BigDecimal baseQty,
            LocalDateTime updatedAt
    ) {
        this.symbol = symbol;
        this.baseQty = baseQty;
        this.updatedAt = updatedAt;
    }
}