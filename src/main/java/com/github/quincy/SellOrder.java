package com.github.quincy;

public class SellOrder implements Trade {
    private final String symbol;
    private final double units;

    public SellOrder(String symbol, double units) {
        this.symbol = symbol;
        this.units = units;
    }

    @Override
    public TradeType getType() {
        return TradeType.SELL;
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
