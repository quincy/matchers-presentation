package com.github.quincy;

public enum TradeType {
    BUY(1),
    SELL(-1);

    /**
     * All trade amounts are expressed in positive values.
     * But when the amount is recorded in the Ledger we need to know in which direction to move the balance.
     * This field gives us the sign adjustment to apply when entering the value into the Ledger.
     */
    private final int adjustmentSign;

    TradeType(int adjustmentSign) {
        this.adjustmentSign = adjustmentSign;
    }

    public int getAdjustmentSign() {
        return adjustmentSign;
    }
}
