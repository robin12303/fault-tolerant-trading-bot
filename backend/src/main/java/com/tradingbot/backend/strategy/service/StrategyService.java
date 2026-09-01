package com.tradingbot.backend.strategy.service;

import com.tradingbot.backend.market.client.BinanceMarketClient;
import com.tradingbot.backend.market.dto.BinanceKline;
import com.tradingbot.backend.market.store.LatestPriceStore;
import com.tradingbot.backend.strategy.dto.EntrySignalResult;
import com.tradingbot.backend.strategy.dto.StrategySnapshot;
import com.tradingbot.backend.strategy.store.StrategySnapshotStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class StrategyService {

    private static final BigDecimal K = new BigDecimal("0.5");

    private final BinanceMarketClient marketClient;
    private final LatestPriceStore latestPriceStore;
    private final StrategySnapshotStore strategySnapshotStore;

    public StrategyService(
            BinanceMarketClient marketClient,
            LatestPriceStore latestPriceStore,
            StrategySnapshotStore strategySnapshotStore
    ) {
        this.marketClient = marketClient;
        this.latestPriceStore = latestPriceStore;
        this.strategySnapshotStore = strategySnapshotStore;
    }

    // 하루에 한 번 전략 기준값 계산
    public StrategySnapshot refresh(String symbol) {

        long serverTime = marketClient.getServerTime();

        // =========================
        // 1. 거래 대상 일봉
        // =========================

        List<BinanceKline> klines =
                marketClient.getDailyKlines(symbol, 7);

        List<BinanceKline> completed = klines.stream()
                .filter(kline -> kline.closeTime() < serverTime)
                .toList();

        List<BinanceKline> currentCandidates = klines.stream()
                .filter(kline ->
                        kline.openTime() <= serverTime
                                && serverTime <= kline.closeTime()
                )
                .toList();

        if (completed.size() < 5) {
            throw new IllegalStateException(
                    "Not enough completed daily candles for " + symbol
            );
        }

        if (currentCandidates.size() != 1) {
            throw new IllegalStateException(
                    "Current daily candle is invalid for " + symbol
            );
        }

        BinanceKline current =
                currentCandidates.get(0);

        List<BinanceKline> last5 =
                completed.subList(
                        completed.size() - 5,
                        completed.size()
                );

        BinanceKline yesterday =
                completed.get(completed.size() - 1);

        BigDecimal sma5 = calculateSma5(last5);

        BigDecimal range =
                yesterday.high()
                        .subtract(yesterday.low());

        BigDecimal target =
                current.open()
                        .add(range.multiply(K));

        // =========================
        // 2. BTC 시장 필터
        // =========================

        List<BinanceKline> btcKlines =
                marketClient.getDailyKlines("BTCUSDT", 7);

        List<BinanceKline> btcCompleted =
                btcKlines.stream()
                        .filter(kline ->
                                kline.closeTime() < serverTime
                        )
                        .toList();

        if (btcCompleted.size() < 5) {
            throw new IllegalStateException(
                    "Not enough completed BTC daily candles"
            );
        }

        List<BinanceKline> btcLast5 =
                btcCompleted.subList(
                        btcCompleted.size() - 5,
                        btcCompleted.size()
                );

        BigDecimal btcSma5 =
                calculateSma5(btcLast5);

        // =========================
        // 3. immutable snapshot 생성
        // =========================

        StrategySnapshot snapshot =
                new StrategySnapshot(
                        symbol,
                        current.open(),
                        yesterday.high(),
                        yesterday.low(),
                        sma5,
                        K,
                        target,
                        btcSma5
                );

        // 완성된 snapshot을 한 번에 교체
        strategySnapshotStore.set(snapshot);

        return snapshot;
    }

    // 장중 실시간 가격으로 진입조건 평가
    public EntrySignalResult evaluate(String symbol) {

        StrategySnapshot strategy =
                strategySnapshotStore.get();

        if (strategy == null) {
            throw new IllegalStateException(
                    "Strategy snapshot is not initialized"
            );
        }

        // 1종목 MVP에서 잘못된 snapshot 사용 방지
        if (!strategy.symbol().equals(symbol)) {
            throw new IllegalStateException(
                    "Strategy snapshot symbol mismatch"
            );
        }

        LatestPriceStore.PriceSnapshot symbolPrice =
                latestPriceStore.get(symbol);

        LatestPriceStore.PriceSnapshot btcPrice =
                latestPriceStore.get("BTCUSDT");

        if (symbolPrice == null || btcPrice == null) {
            throw new IllegalStateException(
                    "WebSocket price is not available"
            );
        }

        if (isStale(symbolPrice) || isStale(btcPrice)) {
            throw new IllegalStateException(
                    "WebSocket market data is stale"
            );
        }

        BigDecimal currentPrice =
                symbolPrice.price();

        BigDecimal btcCurrentPrice =
                btcPrice.price();

        boolean entrySignal =
                currentPrice.compareTo(strategy.target()) >= 0
                        && currentPrice.compareTo(strategy.sma5()) > 0
                        && btcCurrentPrice.compareTo(strategy.btcSma5()) > 0;

        return new EntrySignalResult(
                symbol,
                currentPrice,
                strategy.target(),
                strategy.sma5(),
                btcCurrentPrice,
                strategy.btcSma5(),
                entrySignal
        );
    }

    private BigDecimal calculateSma5(
            List<BinanceKline> klines
    ) {
        if (klines.size() != 5) {
            throw new IllegalArgumentException(
                    "SMA5 requires exactly 5 candles"
            );
        }

        return klines.stream()
                .map(BinanceKline::close)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(
                        BigDecimal.valueOf(5),
                        8,
                        RoundingMode.HALF_UP
                );
    }

    private boolean isStale(
            LatestPriceStore.PriceSnapshot snapshot
    ) {
        return Duration.between(
                snapshot.receivedAt(),
                Instant.now()
        ).toSeconds() > 10;
    }
}