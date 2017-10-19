package com.github.quincy;

/**
 * Bookkeeper accepts Trade orders from the user, attempts to execute them using the Portfolio, and records the successful Transactions in a Ledger.
 */
public class Bookkeeper {
    private final Ledger ledger;
    private final Portfolio portfolio;

    public Bookkeeper(Ledger ledger, Portfolio portfolio) {
        this.ledger = ledger;
        this.portfolio = portfolio;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void submit(Trade... orders) throws MarketClosedException {
        for (Trade order : orders) {
            Transaction transaction = portfolio.trade(order);
            ledger.record(transaction);
        }
    }
}
