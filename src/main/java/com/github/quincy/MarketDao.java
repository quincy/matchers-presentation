package com.github.quincy;

/**
 * Executes a trade in the Market.
 *
 * In this example we simply return the resulting Transaction directly.
 */
public class MarketDao {
    public Transaction execute(Trade trade) {
        return new Transaction(trade.getSymbol(), trade.getUnits());
    }
}
