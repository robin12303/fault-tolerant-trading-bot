package com.tradingbot.backend.trade.fsm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class TradeStateMachineTest {


    @Test
    void buyingCanBecomeBought() {

        TradeStateMachine fsm = new TradeStateMachine();

        assertTrue(fsm.tryStartBuying());

        fsm.markBought();

        assertEquals(
                TradeState.BOUGHT,
                fsm.getState()
        );
    }

    @Test
    void rejectedBuyReturnsToReady() {

        TradeStateMachine fsm = new TradeStateMachine();

        assertTrue(fsm.tryStartBuying());

        fsm.markBuyRejected();

        assertEquals(
                TradeState.READY,
                fsm.getState()
        );
    }

    @Test
    void ambiguousBuyBecomesUnknown() {

        TradeStateMachine fsm = new TradeStateMachine();

        assertTrue(fsm.tryStartBuying());

        fsm.markBuyUnknown();

        assertEquals(
                TradeState.UNKNOWN,
                fsm.getState()
        );
    }

    @Test
    void onlyOneThreadCanStartBuying() throws Exception {

        TradeStateMachine fsm =
                new TradeStateMachine();

        int threadCount = 100;

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        CountDownLatch startGate =
                new CountDownLatch(1);

        List<Future<Boolean>> futures =
                new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {

            futures.add(
                    executor.submit(() -> {

                        // 100개 스레드가 여기서 대기
                        startGate.await();

                        return fsm.tryStartBuying();
                    })
            );
        }

        // 동시에 출발
        startGate.countDown();

        int successCount = 0;

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        executor.shutdown();

        assertEquals(
                1,
                successCount,
                "READY -> BUYING 전환은 정확히 한 번만 성공해야 함"
        );

        assertEquals(
                TradeState.BUYING,
                fsm.getState()
        );
    }


}