package com.github.quincy;

import java.util.Objects;

/**
 * Represents a successful Trade in the market.
 */
public class Transaction {
    private String symbol;
    /** The amount by which the Ledger should be adjusted.  This is signed based on the TradeType. */
    private double unitsAdjustment;

    public Transaction(Trade trade) {
        this.symbol = trade.getSymbol();
        this.unitsAdjustment = trade.getUnits() * trade.getType().getAdjustmentSign();
    }

    public Transaction(String symbol, double unitsAdjustment) {
        this.symbol = symbol;
        this.unitsAdjustment = unitsAdjustment;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getUnitsAdjustment() {
        return unitsAdjustment;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "symbol='" + symbol + '\'' +
                ", unitsAdjustment=" + unitsAdjustment +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Double.compare(that.unitsAdjustment, unitsAdjustment) == 0 &&
                Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, unitsAdjustment);
    }
}
