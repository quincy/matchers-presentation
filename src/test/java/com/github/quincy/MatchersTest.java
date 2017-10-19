package com.github.quincy;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static com.github.quincy.PortfolioMatcher.hasPosition;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * These tests improve on our Legacy versions by using Hamcrest Matchers and some lesser known features of Mockito.
 */
public class MatchersTest {
    private TradeClock clock;
    private MarketDao marketDao;
    private Portfolio portfolio;

    @Before
    public void setUp() {
        clock = mock(TradeClock.class);
        marketDao = mock(MarketDao.class);

        portfolio = new Portfolio.Builder()
                // Given I have 100 shares of MSFT stock
                .withPosition(new Position("MSFT", 100.0))
                //   And I have 150 shares of APPL stock
                .withPosition(new Position("APPL", 150.0))
                .withTradeExecutor(new TradeExecutor(clock, marketDao))
                .build();
    }

    /**
     * Feature: Portfolio trades stocks
     * Scenario: Portfolio requests a sell before close of trading
     *
     * Given I have 100 shares of MSFT stock
     * And I have 150 shares of APPL stock
     * And the time is before close of trading
     *
     * When I ask to sell 20 shares of MSFT stock
     *
     * Then I should have 80 shares of MSFT stock
     * And I should have 150 shares of APPL stock
     * And a sell order for 20 shares of MSFT stock should have been executed
     */
    @Test
    public void userTradesStocks() throws MarketClosedException {
        // Given... And the time is before close of trading
        when(clock.isMarketOpen()).thenReturn(true);

        Trade sellOrder = new SellOrder("MSFT", 20.0);
        when(marketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // When I ask to sell 20 shares of MSFT stock
        portfolio.trade(sellOrder);

        // Then I should have 80 shares of MSFT stock
        assertThat(portfolio, hasPosition(new Position("MSFT", 80.0)));
        //   And I should have 150 shares of APPL stock
        assertThat(portfolio, hasPosition(new Position("APPL", 150.0)));

        //   And a sell order for 20 shares of MSFT stock should have been executed
        verify(marketDao).execute(sellOrder);
    }

    /**
     * Feature: Portfolio attempts to trade stocks after hours
     * Scenario: Portfolio requests a sell after close of trading
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
    @Test
    public void afterHoursTradeIsRejected() throws MarketClosedException {
        // Given... And the time is before close of trading
        when(clock.isMarketOpen()).thenReturn(false);

        try {
            // When I ask to sell 20 shares of MSFT stock
            portfolio.trade(new SellOrder("MSFT", 20.0));
            fail("Expected a MarketClosedException to be thrown.");
        } catch (MarketClosedException e) {
            // Then I should have 100 shares of MSFT stock
            assertThat(portfolio, hasPosition(new Position("MSFT", 100.0)));
            //   And I should have 150 shares of APPL stock
            assertThat(portfolio, hasPosition(new Position("APPL", 150.0)));

            //   And no sell orders should have been executed
            verify(marketDao, never()).execute(any(Trade.class)); // Mockito matcher
        }
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
        // Given the time is before close of trading
        when(clock.isMarketOpen()).thenReturn(true);

        Trade buyOrder = new BuyOrder("APPL", 120.0);
        Trade sellOrder = new SellOrder("APPL", 50.0);
        Transaction buyTransaction = new Transaction("APPL", 120.0);
        Transaction sellTransaction = new Transaction("APPL", -50.0);

        Ledger ledger = mock(Ledger.class);

        Portfolio portfolio = mock(Portfolio.class);
        when(portfolio.trade(eq(buyOrder))).thenReturn(buyTransaction);
        when(portfolio.trade(eq(sellOrder))).thenReturn(sellTransaction);

        Bookkeeper bookkeeper = new Bookkeeper(ledger, portfolio);

        // When I ask to buy 120 shares of APPL stock
        //   And I ask to sell 50 shares of APPL stock
        bookkeeper.submit(buyOrder, sellOrder);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);

        verify(portfolio, times(2)).trade(tradeCaptor.capture());
        List<Trade> trades = tradeCaptor.getAllValues();

        // Then a buy order for 120 shares of APPL stock should have been executed
        assertThat(trades, hasItem(buyOrder));

        //   And a sell order for 50 shares of APPL stock should have been executed
        assertThat(trades, hasItem(sellOrder));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        verify(ledger, times(2)).record(transactionCaptor.capture());
        List<Transaction> transactions = transactionCaptor.getAllValues();

        //   And a transaction for +120 shares of APPL stock should have been recorded in the Ledger
        assertThat(transactions, hasItem(buyTransaction));

        //   And a transaction for -50 shares of APPL stock should have been recorded in the Ledger
        assertThat(transactions, hasItem(sellTransaction));
    }
}
