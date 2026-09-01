package com.tradingbot.backend.trade.service;

import com.tradingbot.backend.trade.order.MockOrderResult;
import org.springframework.stereotype.Service;

@Service
public class MockOrderService {

    public MockOrderResult buy(String symbol) {

        System.out.println("[MOCK BUY] " + symbol);

        // 지금은 정상 체결로 고정
        return MockOrderResult.FILLED;
    }
}