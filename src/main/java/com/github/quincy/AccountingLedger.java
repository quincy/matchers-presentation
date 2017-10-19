package com.github.quincy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AccountingLedger implements Ledger {
    private final List<Transaction> transactions;

    public AccountingLedger() {
        this(Collections.emptyList());
    }

    public AccountingLedger(Collection<Transaction> transactions) {
        this.transactions = new ArrayList<>(transactions);
    }

    @Override
    public List<Transaction> getTransactions() {
        return transactions;
    }

    @Override
    public double changeInBalance(String symbol) {
        return transactions.stream()
                .filter(transaction -> Objects.equals(transaction.getSymbol(), symbol))
                .mapToDouble(Transaction::getUnitsAdjustment)
                .sum();
    }

    @Override
    public void record(Transaction transaction) {
        transactions.add(transaction);
    }

    @Override
    public void record(Transaction... transactions) {
        this.transactions.addAll(Arrays.asList(transactions));
    }
}
