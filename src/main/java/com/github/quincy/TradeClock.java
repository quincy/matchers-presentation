package com.github.quincy;

/**
 * Represents the ability to determine whether or not the stock market is open for trading.
 */
@FunctionalInterface
public interface TradeClock {
    boolean isMarketOpen();
}
