package com.github.quincy;

import org.junit.Test;

import static com.github.quincy.PortfolioMatcher.hasPosition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
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
                .withTradeClock(new Clock())
                .withMarketDao(new MarketDao())
                .build();

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
        MarketDao mockMarketDao = mock(MarketDao.class);
        when(mockMarketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // Use a mock TradeClock so we can control its behavior and run our test at any time of day.
        TradeClock mockClock = mock(TradeClock.class);
        when(mockClock.isMarketOpen()).thenReturn(true);

        Portfolio portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                //   And the time is before close of trading
                .withTradeClock(mockClock)
                .withMarketDao(mockMarketDao)
                .build();

        // When I ask to sell 20 shares of MSFT stock
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertEquals(new Position("MSFT", 80.0), portfolio.getPosition("MSFT").orElse(null));
        //   And I should have 150 shares of APPL stock
        assertEquals(new Position("APPL", 150.0), portfolio.getPosition("APPL").orElse(null));

        // This last part must have happened right?
        //   And a sell order for 20 shares of MSFT stock should have been executed
    }

    @Test
    public void userTradesStocks_v3() throws MarketClosedException {
        Trade sellOrder = new SellOrder("MSFT", 20.0);

        // Use a mock MarketDao to avoid real trades being executed from our test!
        MarketDao mockMarketDao = mock(MarketDao.class);
        when(mockMarketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // Use a mock TradeClock so we can control its behavior and run our test at any time of day.
        TradeClock mockClock = mock(TradeClock.class);
        when(mockClock.isMarketOpen()).thenReturn(true);

        Portfolio portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                //   And the time is before close of trading
                .withTradeClock(mockClock)
                .withMarketDao(mockMarketDao)
                .build();

        // When I ask to sell 20 shares of MSFT stock
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertEquals(new Position("MSFT", 80.0), portfolio.getPosition("MSFT").orElse(null));
        //   And I should have 150 shares of APPL stock
        assertEquals(new Position("APPL", 150.0), portfolio.getPosition("APPL").orElse(null));

        //   And a sell order for 20 shares of MSFT stock should have been executed
        verify(mockMarketDao).execute(sellOrder);
    }

    @Test
    public void userTradesStocks_v4() throws MarketClosedException {
        Trade sellOrder = new SellOrder("MSFT", 20.0);

        // Use a mock MarketDao to avoid real trades being executed from our test!
        MarketDao mockMarketDao = mock(MarketDao.class);
        when(mockMarketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // Use a mock TradeClock so we can control its behavior and run our test at any time of day.
        TradeClock mockClock = mock(TradeClock.class);
        when(mockClock.isMarketOpen()).thenReturn(true);

        Portfolio portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                //   And the time is before close of trading
                .withTradeClock(mockClock)
                .withMarketDao(mockMarketDao)
                .build();

        // When I ask to sell 20 shares of MSFT stock
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertThat(portfolio, hasPosition(new Position("MSFT", 80.0)));
        //   And I should have 150 shares of APPL stock
        assertThat(portfolio, hasPosition(new Position("APPL", 150.0)));

        //   And a sell order for 20 shares of MSFT stock should have been executed
        verify(mockMarketDao).execute(sellOrder);
    }
}

