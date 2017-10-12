package com.github.quincy;

import java.util.Objects;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static java.util.stream.Collectors.joining;

/**
 * Provides Matchers for instances of {@link Ledger}.
 */
public class LedgerMatcher {

    public static TypeSafeMatcher<Ledger> hasTransaction(Transaction expectedTransaction) {
        return new TypeSafeMatcher<Ledger>() {
            @Override
            protected boolean matchesSafely(Ledger ledger) {
                return ledger.getTransactions().stream()
                        .anyMatch(transaction -> Objects.equals(transaction, expectedTransaction));
            }

            @Override
            public void describeTo(Description description) {
                // describe what is expected
                description.appendText("Ledger should contain a " + expectedTransaction);
            }

            @Override
            protected void describeMismatchSafely(Ledger ledger, Description mismatchDescription) {
                // describe what was actually found
                mismatchDescription.appendText("Ledger contains ");
                mismatchDescription.appendText(ledger.getTransactions().stream().map(Transaction::toString).collect(joining("\n")));
            }
        };
    }

    public static TypeSafeMatcher<Ledger> changedBy(String symbol, double expectedAmount) {
        return new TypeSafeMatcher<Ledger>() {
            @Override
            protected boolean matchesSafely(Ledger ledger) {
                return ledger.changeInBalance(symbol) == expectedAmount;
            }

            @Override
            public void describeTo(Description description) {
                // describe what is expected
                description.appendText("Ledger should have changed by " + expectedAmount);
            }

            @Override
            protected void describeMismatchSafely(Ledger ledger, Description mismatchDescription) {
                // describe what was actually found
                mismatchDescription.appendText("Ledger actually changed by ");
                mismatchDescription.appendValue(ledger.changeInBalance(symbol));
                mismatchDescription.appendText(" and contains the following Transactions for balance " + symbol + "\n");
                mismatchDescription.appendText(ledger.getTransactions().stream()
                        .filter(transaction -> Objects.equals(symbol, transaction.getSymbol()))
                        .map(Transaction::toString)
                        .collect(joining("\n")));
            }
        };
    }
}
