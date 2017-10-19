package com.github.quincy;

import java.util.List;

/**
 * Ledger tracks the change of balances over time.
 */
public interface Ledger {
    List<Transaction> getTransactions();

    double changeInBalance(String symbol);

    void record(Transaction transaction);

    void record(Transaction... transactions);
}
