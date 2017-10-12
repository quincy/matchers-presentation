package com.github.quincy;

public class BuyOrder implements Trade {
    private final String symbol;
    private final double units;

    public BuyOrder(String symbol, double units) {
        this.symbol = symbol;
        this.units = units;
    }

    @Override
    public TradeType getType() {
        return TradeType.BUY;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public double getUnits() {
        return units;
    }
}
