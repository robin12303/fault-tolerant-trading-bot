package com.tradingbot.backend.trade.position;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository
    extends JpaRepository<PositionEntity,String>{
}
