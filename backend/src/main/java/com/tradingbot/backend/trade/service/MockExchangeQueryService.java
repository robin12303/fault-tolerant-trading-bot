package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.order.MockExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.MockExchangeOrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MockExchangeQueryService {

    public MockExchangeOrderSnapshot queryBuyOrder(String symbol) {

        // 테스트용
        return new MockExchangeOrderSnapshot(
                MockExchangeOrderStatus.FILLED,
                new BigDecimal("0.05")
        );
    }
}