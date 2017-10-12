package com.github.quincy;

public class MarketClosedException extends Throwable {
    @Override
    public String getMessage() {
        return "The market is currently closed.";
    }
}
