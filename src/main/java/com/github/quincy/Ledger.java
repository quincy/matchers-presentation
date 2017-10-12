package com.github.quincy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ledger tracks the change in ownership of a stock over time.
 */
public class Ledger {
    private final List<Transaction> transactions;

    public Ledger() {
        this(Collections.emptyList());
    }

    public Ledger(Collection<Transaction> transactions) {
        this.transactions = new ArrayList<>(transactions);
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public double changeInBalance(String symbol) {
        return transactions.stream()
                .filter(transaction -> Objects.equals(transaction.getSymbol(), symbol))
                .mapToDouble(Transaction::getUnitsAdjustment)
                .sum();
    }

    public void record(Transaction transaction) {
        transactions.add(transaction);
    }
}
