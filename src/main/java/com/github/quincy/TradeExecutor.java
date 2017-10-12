package com.github.quincy;

/**
 * Executes a Trades by sending a request to the provided MarketDao.
 *
 * Trades can only be executed while the Market is open.
 */
public class TradeExecutor {
    private final TradeClock clock;
    private final MarketDao marketDao;

    public TradeExecutor(TradeClock clock, MarketDao marketDao) {
        this.clock = clock;
        this.marketDao = marketDao;
    }

    /**
     * Executes a given Trade if the Market is currently open.
     *
     * @param trade the trade to execute.
     * @return a Transaction if the Trade was successful.
     * @throws MarketClosedException if the Market is not currently open for trading.
     */
    public Transaction execute(Trade trade) throws MarketClosedException {
        if (clock.isMarketOpen()) {
            return marketDao.execute(trade);
        }

        throw new MarketClosedException();
    }
}
