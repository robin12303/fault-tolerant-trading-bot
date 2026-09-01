package com.tradingbot.backend.trade.order;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClientOrderIdGenerator {

    public String next() {

        return "bot-"
                + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}