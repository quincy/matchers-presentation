package com.github.quincy;

import java.util.Collections;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Here is what a typical test might look like when you are first getting started with JUnit and Mockito.
 */
public class LegacyTest {
    private TradeClock clock;
    private MarketDao marketDao;
    private Portfolio portfolio;

    @Before
    public void setUp() {
        clock = mock(TradeClock.class);
        marketDao = mock(MarketDao.class);

        // Given
        portfolio = new Portfolio.Builder()
                .withPosition(new Position("MSFT", 100.0))
                .withPosition(new Position("APPL", 150.0))
                .withTradeExecutor(new TradeExecutor(clock, marketDao))
                .build();
    }

    /**
     * Feature: User trades stocks
     * Scenario: User requests a sell before close of trading
     * <p>
     * Given I have 100 shares of MSFT stock
     * And I have 150 shares of APPL stock
     * And the time is before close of trading
     * <p>
     * When I ask to sell 20 shares of MSFT stock
     * <p>
     * Then I should have 80 shares of MSFT stock
     * And I should have 150 shares of APPL stock
     * And a sell order for 20 shares of MSFT stock should have been executed
     */
    @Test
    public void userTradesStocks() throws MarketClosedException {
        // Given
        when(clock.isMarketOpen()).thenReturn(true);

        Trade sellOrder = new SellOrder("MSFT", 20.0);
        when(marketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // When
        portfolio.trade(sellOrder);

        // Then
        assertEquals(new Position("MSFT", 80.0), portfolio.getPosition("MSFT").orElse(null));
        assertEquals(new Position("APPL", 150.0), portfolio.getPosition("APPL").orElse(null));

        // This last part must have happened right?
        // And a sell order for 20 shares of MSFT stock should have been executed
    }

    /**
     * Feature: User attempts to trade stocks after hours
     * Scenario: User requests a sell after close of trading
     *
     * Given I have 100 shares of MSFT stock
     * And I have 150 shares of APPL stock
     * And the time is after close of trading
     *
     * When I ask to sell 20 shares of MSFT stock
     *
     * Then I should have 100 shares of MSFT stock
     * And I should have 150 shares of APPL stock
     * And no sell orders should have been executed
     */
    @Test(expected = MarketClosedException.class)
    public void afterHoursTradeIsRejected() throws MarketClosedException {
        // Given
        Trade sellOrder = new SellOrder("MSFT", -20.0);

        // When
        portfolio.trade(sellOrder);

        // Then
        // And no sell orders should have been executed
    }

    /**
     * Feature: User buys and sells some stocks
     * Scenario: User requests a buy and a sell before close of trading
     *
     * Given I have 100 shares of MSFT stock
     * And I have 150 shares of APPL stock
     * And the time is after close of trading
     *
     * When I ask to buy 120 shares of APPL stock
     * And I ask to sell 50 shares of APPL stock
     *
     * Then I should have 100 shares of MSFT stock
     * And I should have 220 shares of APPL stock
     * And a buy order for 120 shares of APPL stock should have been executed
     * And a transaction for +120 shares of APPL stock should have been recorded in the Ledger
     * And a sell order for 50 shares of APPL stock should have been executed
     * And a transaction for -50 shares of APPL stock should have been recorded in the Ledger
     */
    @Test
    public void userTradesMultipleStocks() throws MarketClosedException {
        // Given
        when(clock.isMarketOpen()).thenReturn(true);

        Trade buyOrder = new BuyOrder("APPL", 120.0);
        Trade sellOrder = new SellOrder("APPL", 50.0);
        when(marketDao.execute(buyOrder)).thenReturn(new Transaction(buyOrder));
        when(marketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        Ledger ledger = new Ledger();
        Bookkeeper bookkeeper = new Bookkeeper(ledger, portfolio);

        // When
        bookkeeper.submit(buyOrder);
        bookkeeper.submit(sellOrder);

        // Then
        assertEquals(2, ledger.getTransactions().stream()
                .filter(transaction -> transaction.getSymbol().equals("APPL"))
                .mapToDouble(Transaction::getUnitsAdjustment)
                .filter(amount -> amount == -50.0 || amount == 120.0)
                .count());

        assertEquals(0, portfolio.getPositions().stream()
                .filter(position -> !Objects.equals(new Position("MSFT", 100.0), position))
                .filter(position -> !Objects.equals(new Position("APPL", 220.0), position))
                .count());

        // And a buy order for 120 shares of APPL stock should have been executed
        // And a sell order for 50 shares of APPL stock should have been executed
    }

    /**
     * Feature: The Ledger correctly tracks the change in balance over time
     * Scenario: One buy and one sell transactions are entered into a Ledger which already has an entry
     *
     * Given I have a transaction for +100 on MSFT
     *
     * When I enter a transaction for +120 on MSFT
     * And I enter a transaction for -50 on MSFT
     *
     * Then I should have a total change in balance of 170 on MSFT
     */
    @Test
    public void ledgerTracksChangeInBalanceOverTime() {
        // Given I have a transaction for +100 on MSFT
        Transaction originalTransaction = new Transaction("MSFT", 100.0);
        Ledger ledger = new Ledger(Collections.singletonList(originalTransaction));

        // When I enter a transaction for +120 on MSFT
        // And I enter a transaction for -50 on MSFT
        ledger.record(new Transaction("MSFT", 120.0));
        ledger.record(new Transaction("MSFT", -50.0));

        // Then I should have a total change in balance of 170 on MSFT
        assertEquals(170.0, ledger.changeInBalance("MSFT"), 0.0001);
    }
}
