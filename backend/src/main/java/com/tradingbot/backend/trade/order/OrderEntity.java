package com.tradingbot.backend.trade.order;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "client_order_id",
            nullable = false,
            unique = true
    )
    private String clientOrderId;

    @Column(name = "exchange_order_id")
    private Long exchangeOrderId;

    @Column(
            name = "symbol",
            nullable = false
    )
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "side",
            nullable = false
    )
    private OrderSide side;

    @Column(
            name = "type",
            nullable = false
    )
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    private OrderJournalStatus status;

    @Column(name = "requested_quote_qty")
    private BigDecimal requestedQuoteQty;

    @Column(name = "requested_base_qty")
    private BigDecimal requestedBaseQty;

    @Column(
            name = "executed_qty",
            nullable = false
    )
    private BigDecimal executedQty;

    @Column(
            name = "executed_quote_qty",
            nullable = false
    )
    private BigDecimal executedQuoteQty;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    protected OrderEntity() {
    }

    public OrderEntity(
            String clientOrderId,
            String symbol,
            OrderSide side,
            BigDecimal requestedQuoteQty,
            LocalDateTime now
    ) {
        this.clientOrderId = clientOrderId;
        this.symbol = symbol;
        this.side = side;

        this.type = "MARKET";
        this.status =
                OrderJournalStatus.PENDING_SUBMISSION;

        this.requestedQuoteQty =
                requestedQuoteQty;

        this.requestedBaseQty =
                null;

        this.executedQty =
                BigDecimal.ZERO;

        this.executedQuoteQty =
                BigDecimal.ZERO;

        this.createdAt = now;
        this.updatedAt = now;
    }

    public void applySubmissionResult(
            OrderSubmissionResult result,
            LocalDateTime now
    ) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Order submission result must not be null"
            );
        }

        if (!clientOrderId.equals(
                result.clientOrderId()
        )) {
            throw new IllegalStateException(
                    "clientOrderId mismatch"
            );
        }

        this.exchangeOrderId =
                result.exchangeOrderId();

        this.status =
                switch (result.status()) {
                    case ACCEPTED ->
                            OrderJournalStatus.ACCEPTED;

                    case REJECTED ->
                            OrderJournalStatus.REJECTED;

                    case RATE_LIMITED ->
                            OrderJournalStatus.RATE_LIMITED;

                    case UNKNOWN ->
                            OrderJournalStatus.UNKNOWN;
                };

        this.updatedAt = now;
    }
}