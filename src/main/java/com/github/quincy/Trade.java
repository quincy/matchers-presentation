package com.github.quincy;

public interface Trade {
    TradeType getType();
    String getSymbol();
    double getUnits();
}
