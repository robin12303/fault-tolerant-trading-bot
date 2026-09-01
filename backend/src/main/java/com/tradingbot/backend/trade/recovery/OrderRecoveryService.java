package com.tradingbot.backend.trade.recovery;

import com.tradingbot.backend.trade.order.ExchangeOrderSnapshot;
import com.tradingbot.backend.trade.order.OrderEntity;
import com.tradingbot.backend.trade.order.OrderJournalService;
import com.tradingbot.backend.trade.order.OrderQueryClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderRecoveryService {

    private final OrderJournalService orderJournalService;
    private final OrderQueryClient orderQueryClient;

    public OrderRecoveryService(
            OrderJournalService orderJournalService,
            OrderQueryClient orderQueryClient
    ) {
        this.orderJournalService =
                orderJournalService;

        this.orderQueryClient =
                orderQueryClient;
    }

    public StartupReconciliationResult recover() {

        final List<OrderEntity> orders;

        try {
            orders =
                    orderJournalService
                            .findRecoverableOrders();

        } catch (RuntimeException e) {

            return StartupReconciliationResult.UNAVAILABLE;
        }

        StartupReconciliationResult overall =
                StartupReconciliationResult.CONSISTENT;

        for (OrderEntity order : orders) {

            try {

                ExchangeOrderSnapshot snapshot =
                        orderQueryClient.getOrder(
                                order.getSymbol(),
                                order.getClientOrderId()
                        );

                orderJournalService
                        .recordExchangeSnapshot(
                                order.getClientOrderId(),
                                snapshot
                        );

                switch (snapshot.status()) {

                    case FILLED,
                         REJECTED,
                         CANCELED,
                         EXPIRED,
                         EXPIRED_IN_MATCH -> {
                        // terminal
                    }

                    case NEW,
                         PARTIALLY_FILLED,
                         NOT_FOUND -> {

                        if (overall
                                == StartupReconciliationResult.CONSISTENT) {

                            overall =
                                    StartupReconciliationResult.INCONSISTENT;
                        }
                    }

                    case UNKNOWN -> {

                        overall =
                                StartupReconciliationResult.UNAVAILABLE;
                    }
                }

            } catch (RuntimeException e) {

                /*
                 * 한 주문 조회가 실패했다고
                 * 나머지 주문 복구까지 포기하지 않는다.
                 *
                 * 하지만 전체 startup 판정은
                 * 반드시 fail-closed.
                 */
                overall =
                        StartupReconciliationResult.UNAVAILABLE;
            }
        }

        return overall;
    }
}