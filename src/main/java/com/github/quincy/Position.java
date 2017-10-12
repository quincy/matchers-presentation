package com.github.quincy;

import java.util.Objects;

/**
 * Represents a user's total ownership of a particular stock.
 */
public class Position {
    private final String symbol;
    private final double units;

    public Position(String symbol, double units) {
        this.symbol = symbol;
        this.units = units;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getUnits() {
        return units;
    }

    @Override
    public String toString() {
        return "Position{" +
                "symbol='" + symbol + '\'' +
                ", units=" + units +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return Double.compare(position.units, units) == 0 &&
                Objects.equals(symbol, position.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, units);
    }
}
