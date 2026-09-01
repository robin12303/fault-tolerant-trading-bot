package com.tradingbot.backend.trade.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;

@Service
public class OrderJournalService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    public OrderJournalService(
            OrderRepository orderRepository,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public OrderEntity recordSubmissionResult(
            String clientOrderId,
            OrderSubmissionResult result
    ) {

        if (clientOrderId == null
                || clientOrderId.isBlank()) {

            throw new IllegalArgumentException(
                    "clientOrderId must not be blank"
            );
        }

        OrderEntity order =
                orderRepository
                        .findByClientOrderId(
                                clientOrderId
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Order journal not found: "
                                                        + clientOrderId
                                        )
                        );

        LocalDateTime now =
                LocalDateTime.ofInstant(
                        clock.instant(),
                        ZoneOffset.UTC
                );

        order.applySubmissionResult(
                result,
                now
        );

        return orderRepository
                .saveAndFlush(order);
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public OrderEntity recordPending(
            OrderRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Order request must not be null"
            );
        }

        if (request.clientOrderId() == null
                || request.clientOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "clientOrderId must not be blank"
            );
        }

        if (request.symbol() == null
                || request.symbol().isBlank()) {

            throw new IllegalArgumentException(
                    "symbol must not be blank"
            );
        }

        LocalDateTime now =
                LocalDateTime.ofInstant(
                        clock.instant(),
                        ZoneOffset.UTC
                );

        OrderEntity order =
                new OrderEntity(
                        request.clientOrderId(),
                        request.symbol(),
                        request.side(),
                        request.quoteQuantity(),
                        now
                );

        return orderRepository.saveAndFlush(order);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> findRecoverableOrders() {

        return orderRepository
                .findByStatusInOrderByCreatedAtAsc(
                        EnumSet.of(
                                OrderJournalStatus.PENDING_SUBMISSION,
                                OrderJournalStatus.ACCEPTED,
                                OrderJournalStatus.RATE_LIMITED,
                                OrderJournalStatus.UNKNOWN
                        )
                );
    }
}