package com.tradingbot.backend.trade.order;

public interface OrderClient {

    OrderSubmissionResult submit(OrderRequest request);
}