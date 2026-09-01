package com.tradingbot.backend.strategy.controller;

import com.tradingbot.backend.strategy.dto.EntrySignalResult;
import com.tradingbot.backend.strategy.dto.StrategySnapshot;
import com.tradingbot.backend.strategy.service.StrategyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/strategy")
public class StrategyController {

    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @PostMapping("/{symbol}/refresh")
    public StrategySnapshot refresh(
            @PathVariable String symbol
    ) {
        return strategyService.refresh(symbol);
    }

    @GetMapping("/{symbol}/evaluate")
    public EntrySignalResult evaluate(
            @PathVariable String symbol
    ) {
        return strategyService.evaluate(symbol);
    }
}