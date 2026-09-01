package com.tradingbot.backend.trade.order;

import org.springframework.stereotype.Service;

@Service
public class OrderSubmissionCoordinator {

    private final OrderJournalService orderJournalService;
    private final OrderClient orderClient;

    public OrderSubmissionCoordinator(
            OrderJournalService orderJournalService,
            OrderClient orderClient
    ) {
        this.orderJournalService =
                orderJournalService;

        this.orderClient =
                orderClient;
    }

    public OrderSubmissionResult submit(
            OrderRequest request
    ) {

        // 1. Binance보다 먼저 DB에 주문 의도 기록
        orderJournalService
                .recordPending(request);

        // 2. DB 기록이 성공한 뒤에만 외부 주문 전송
        OrderSubmissionResult result =
                orderClient.submit(request);

        // 3. Binance의 응답 결과를 DB에 기록
        orderJournalService
                .recordSubmissionResult(
                        request.clientOrderId(),
                        result
                );

        return result;
    }
}