package com.tradingbot.backend.trade.fsm;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class TradeStateMachine {

    private final AtomicReference<TradeState> state =
            new AtomicReference<>(TradeState.READY);

    public void reconcileToBought() {

        TradeState current =
                state.get();

        if (current != TradeState.UNKNOWN
                && current != TradeState.BUY_SUBMITTED) {

            throw new IllegalStateException(
                    "Invalid reconcile transition to BOUGHT from "
                            + current
            );
        }

        if (!state.compareAndSet(
                current,
                TradeState.BOUGHT
        )) {
            throw new IllegalStateException(
                    "Concurrent state change during reconciliation"
            );
        }
    }

    public void reconcileToReady() {

        TradeState current =
                state.get();

        if (current != TradeState.UNKNOWN
                && current != TradeState.BUY_SUBMITTED) {

            throw new IllegalStateException(
                    "Invalid reconcile transition to READY from "
                            + current
            );
        }

        if (!state.compareAndSet(
                current,
                TradeState.READY
        )) {
            throw new IllegalStateException(
                    "Concurrent state change during reconciliation"
            );
        }
    }

    public TradeState getState() {
        return state.get();
    }

    public boolean tryStartBuying() {
        return state.compareAndSet(
                TradeState.READY,
                TradeState.BUYING
        );
    }

    public void markBought() {
        if (!state.compareAndSet(
                TradeState.BUYING,
                TradeState.BOUGHT
        )) {
            throw new IllegalStateException(
                    "Invalid transition to BOUGHT from " + state.get()
            );
        }
    }

    public void markUnknown() {
        state.set(TradeState.UNKNOWN);
    }

    public void markBuyRejected() {

        if (!state.compareAndSet(
                TradeState.BUYING,
                TradeState.READY
        )) {
            throw new IllegalStateException(
                    "Invalid transition to READY from " + state.get()
            );
        }
    }

    public void markBuyUnknown() {

        if (!state.compareAndSet(
                TradeState.BUYING,
                TradeState.UNKNOWN
        )) {
            throw new IllegalStateException(
                    "Invalid transition to UNKNOWN from " + state.get()
            );
        }
    }

    public void markBuySubmitted() {

        if (!state.compareAndSet(
                TradeState.BUYING,
                TradeState.BUY_SUBMITTED
        )) {
            throw new IllegalStateException(
                    "Invalid transition to BUY_SUBMITTED from "
                            + state.get()
            );
        }
    }

    public void reconcileToUnknown() {

        TradeState current =
                state.get();

        if (current == TradeState.UNKNOWN) {
            return;
        }

        if (current != TradeState.BUY_SUBMITTED) {
            throw new IllegalStateException(
                    "Invalid reconcile transition to UNKNOWN from "
                            + current
            );
        }

        if (!state.compareAndSet(
                TradeState.BUY_SUBMITTED,
                TradeState.UNKNOWN
        )) {
            throw new IllegalStateException(
                    "Concurrent state change during reconciliation"
            );
        }
    }

}