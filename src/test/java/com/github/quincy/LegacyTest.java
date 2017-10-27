package com.github.quincy;

import org.junit.Test;
import org.mockito.Mockito;

import static com.github.quincy.LedgerMatcher.hasTransaction;
import static com.github.quincy.PortfolioMatcher.hasPosition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Here is what a typical test might look like when you are first getting started with JUnit and Mockito.
 */
public class LegacyTest {
    /**
     * Feature: Portfolio trades stocks
     * Scenario: Portfolio requests a sell before close of trading
     *
     * Given I have 100 shares of MSFT stock
     *   And I have 150 shares of APPL stock
     *   And the time is before close of trading
     *
     * When I ask to sell 20 shares of MSFT stock
     *
     * Then I should have 80 shares of MSFT stock
     *   And I should have 150 shares of APPL stock
     *   And a sell order for 20 shares of MSFT stock should have been executed
     */
    @Test
    public void userTradesStocks_v1() throws MarketClosedException {
        Portfolio portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                //   And the time is before close of trading
                .withTradeExecutor(new TradeExecutor(new Clock(), new MarketDao())).build();

        // When I ask to sell 20 shares of MSFT stock
        Trade sellOrder = new SellOrder("MSFT", 20.0);
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertEquals(new Position("MSFT", 80.0), portfolio.getPosition("MSFT").orElse(null));
        //   And I should have 150 shares of APPL stock
        assertEquals(new Position("APPL", 150.0), portfolio.getPosition("APPL").orElse(null));

        // This last part must have happened right?
        //   And a sell order for 20 shares of MSFT stock should have been executed
    }

    @Test
    public void userTradesStocks_v2() throws MarketClosedException {
        Trade sellOrder = new SellOrder("MSFT", 20.0);

        // Use a mock MarketDao to avoid real trades being executed from our test!
        MarketDao marketDao = mock(MarketDao.class);
        when(marketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // Use a mock TradeClock so we can control its behavior and run our test at any time of day.
        TradeClock clock = mock(TradeClock.class);
        when(clock.isMarketOpen()).thenReturn(true);

        Portfolio portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                //   And the time is before close of trading
                .withTradeExecutor(new TradeExecutor(clock, marketDao)).build();

        // When I ask to sell 20 shares of MSFT stock
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertEquals(new Position("MSFT", 80.0), portfolio.getPosition("MSFT").orElse(null));
        //   And I should have 150 shares of APPL stock
        assertEquals(new Position("APPL", 150.0), portfolio.getPosition("APPL").orElse(null));

        // This last part must have happened right?
        //   And a sell order for 20 shares of MSFT stock should have been executed
    }

    /**
     * Feature: Portfolio attempts to trade stocks after hours
     * Scenario: Portfolio requests a sell after close of trading
     *
     * Given I have 100 shares of MSFT stock
     *   And I have 150 shares of APPL stock
     *   And the time is after close of trading
     *
     * When I ask to sell 20 shares of MSFT stock
     *
     * Then I should have 100 shares of MSFT stock
     *   And I should have 150 shares of APPL stock
     *   And no sell orders should have been executed
     */
    @Test(expected = MarketClosedException.class)
    public void afterHoursTradeIsRejected() throws MarketClosedException {
        // Given... And the time is before close of trading
        when(clock.isMarketOpen()).thenReturn(false);

        // When I ask to sell 20 shares of MSFT stock
        Trade sellOrder = new SellOrder("MSFT", 20.0);
        portfolio.trade(sellOrder);

        // No way to verify these things...
        // Then I should have 100 shares of MSFT stock
        //   And I should have 150 shares of APPL stock
        //   And no sell orders should have been executed
    }

    /**
     * Feature: User buys and sells some stocks
     * Scenario: User requests a buy and a sell before close of trading
     *
     * Given the time is before close of trading
     *
     * When I ask to buy 120 shares of APPL stock
     *   And I ask to sell 50 shares of APPL stock
     *
     * Then a buy order for 120 shares of APPL stock should have been executed
     *   And a sell order for 50 shares of APPL stock should have been executed
     *   And a transaction for +120 shares of APPL stock should have been recorded in the Ledger
     *   And a transaction for -50 shares of APPL stock should have been recorded in the Ledger
     */
    @Test
    public void userTradesMultipleStocks() throws MarketClosedException {
        when(clock.isMarketOpen()).thenReturn(true);

        Trade buyOrder = new BuyOrder("APPL", 120.0);
        Trade sellOrder = new SellOrder("APPL", 50.0);
        when(marketDao.execute(eq(buyOrder))).thenReturn(new Transaction(buyOrder));
        when(marketDao.execute(eq(sellOrder))).thenReturn(new Transaction(sellOrder));

        Ledger ledger = Mockito.spy(new AccountingLedger());
        Bookkeeper bookkeeper = new Bookkeeper(ledger, portfolio);

        // When I ask to buy 120 shares of APPL stock
        //   And I ask to sell 50 shares of APPL stock
        bookkeeper.submit(buyOrder, sellOrder);

        // Then a buy order for 120 shares of APPL stock should have been executed
        verify(marketDao).execute(buyOrder);
        //   And a sell order for 50 shares of APPL stock should have been executed
        verify(marketDao).execute(sellOrder);

        //   And a transaction for +120 shares of APPL stock should have been recorded in the Ledger
        assertThat(ledger, hasTransaction(new Transaction("APPL", 120.0)));
        //   And a transaction for -50 shares of APPL stock should have been recorded in the Ledger
        assertThat(ledger, hasTransaction(new Transaction("APPL", -50.0)));

        assertThat(portfolio, hasPosition(new Position("MSFT", 100.0)));
        assertThat(portfolio, hasPosition(new Position("APPL", 220.0)));
    }
}

