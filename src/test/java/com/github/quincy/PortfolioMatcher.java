package com.github.quincy;

import java.util.Optional;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static java.util.stream.Collectors.joining;

/**
 * Provides Matchers for instances of {@link Portfolio}.
 */
public class PortfolioMatcher {

    public static TypeSafeMatcher<Portfolio> hasPosition(Position expectedPosition) {
        return new TypeSafeMatcher<Portfolio>() {
            @Override
            protected boolean matchesSafely(Portfolio portfolio) {
                Optional<Position> maybePosition = portfolio.getPosition(expectedPosition.getSymbol());
                return maybePosition.map(gotPosition -> gotPosition.equals(expectedPosition)).orElse(false);
            }

            @Override
            public void describeTo(Description description) {
                // describe what is expected
                description.appendText("Portfolio should contain a ").appendValue(expectedPosition);
            }

            @Override
            protected void describeMismatchSafely(Portfolio portfolio, Description mismatchDescription) {
                // describe what was actually found
                mismatchDescription.appendText("Portfolio contains ");
                mismatchDescription.appendText(portfolio.getPositions().stream().map(Position::toString).collect(joining("\n")));
            }
        };
    }
}
