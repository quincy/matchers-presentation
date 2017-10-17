package com.github.quincy;

import java.util.Collections;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static com.github.quincy.LedgerMatcher.changedBy;
import static com.github.quincy.LedgerMatcher.hasTransaction;
import static com.github.quincy.PortfolioMatcher.hasPosition;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        // Given
        when(clock.isMarketOpen()).thenReturn(true);

        Trade sellOrder = new SellOrder("MSFT", 20.0);
        when(marketDao.execute(sellOrder)).thenReturn(new Transaction(sellOrder));

        // When
        portfolio.trade(sellOrder);

        // Then
        assertThat(portfolio, hasPosition(new Position("MSFT", 80.0)));
        assertThat(portfolio, hasPosition(new Position("APPL", 150.0)));

        verify(marketDao).execute(sellOrder);
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
    @Test
    public void afterHoursTradeIsRejected() throws MarketClosedException {
        //Given
        when(clock.isMarketOpen()).thenReturn(false);

        try {
            // When
            portfolio.trade(new SellOrder("MSFT", 20.0));
            fail("Expected a MarketClosedException to be thrown.");
        } catch (MarketClosedException e) {
            // Then
            assertThat(portfolio, hasPosition(new Position("MSFT", 100.0)));
            assertThat(portfolio, hasPosition(new Position("APPL", 150.0)));

            verify(marketDao, never()).execute(any(Trade.class)); // Mockito matcher
        }
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
        when(clock.isMarketOpen()).thenReturn(true);

        Trade buyOrder = new BuyOrder("APPL", 120.0);
        Trade sellOrder = new SellOrder("APPL", 50.0);
        when(marketDao.execute(eq(buyOrder))).thenReturn(new Transaction(buyOrder));
        when(marketDao.execute(eq(sellOrder))).thenReturn(new Transaction(sellOrder));

        Ledger ledger = Mockito.spy(new Ledger());
        Bookkeeper bookkeeper = new Bookkeeper(ledger, portfolio);

        // When
        bookkeeper.submit(buyOrder);
        bookkeeper.submit(sellOrder);

        // Then
        assertThat(portfolio, hasPosition(new Position("MSFT", 100.0)));
        assertThat(portfolio, hasPosition(new Position("APPL", 220.0)));

        verify(marketDao).execute(buyOrder);
        verify(marketDao).execute(sellOrder);

        assertThat(ledger, hasTransaction(new Transaction("APPL", 120.0)));
        assertThat(ledger, hasTransaction(new Transaction("APPL", -50.0)));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(ledger, times(2)).record(transactionCaptor.capture());
        assertThat(transactionCaptor.getAllValues(), CoreMatchers.hasItems(
                new Transaction("APPL", 120.0),
                new Transaction("APPL", -50.0)));
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
        // Given
        Transaction originalTransaction = new Transaction("MSFT", 100.0);
        Ledger ledger = new Ledger(Collections.singletonList(originalTransaction));

        // When
        ledger.record(new Transaction("MSFT", 120.0));
        ledger.record(new Transaction("MSFT", -50.0));

        // Then
        assertThat(ledger, changedBy("MSFT", 170.0));
    }
}
