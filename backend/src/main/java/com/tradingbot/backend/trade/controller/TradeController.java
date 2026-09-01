package com.tradingbot.backend.trade.controller;

import com.tradingbot.backend.trade.fsm.TradeStateMachine;
import com.tradingbot.backend.trade.service.TradeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade")
public class TradeController {

    private final TradeService tradeService;
    private final TradeStateMachine fsm;

    public TradeController(
            TradeService tradeService,
            TradeStateMachine fsm
    ) {
        this.tradeService = tradeService;
        this.fsm = fsm;
    }

    @PostMapping("/{symbol}/check")
    public String check(
            @PathVariable String symbol
    ) {
        tradeService.checkAndBuy(symbol);

        return fsm.getState().name();
    }

    @GetMapping("/state")
    public String state() {
        return fsm.getState().name();
    }
}