package com.github.quincy;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A container for all of the {@link Position}s owned by a user.
 *
 * This object represents the current total ownership of stocks for a user.  The user interacts with the Portfolio in order to execute trades.
 */
public class Portfolio {
    private final Map<String, Position> positions;
    private final TradeClock clock;
    private final MarketDao marketDao;

    public Portfolio(Map<String, Position> positions, TradeClock clock, MarketDao marketDao) {
        this.positions = positions;
        this.clock = clock;
        this.marketDao = marketDao;
    }

    public Optional<Position> getPosition(String symbol) {
        return Optional.ofNullable(positions.get(symbol));
    }

    public Set<Position> getPositions() {
        return new HashSet<>(positions.values());
    }

    public Transaction trade(Trade order) throws MarketClosedException {
        if (clock.isMarketOpen()) {
            Transaction transaction = marketDao.execute(order);
            applyTransaction(transaction);
            return transaction;
        }

        throw new MarketClosedException();
    }

    private void applyTransaction(Transaction transaction) {
        positions.compute(transaction.getSymbol(),
                (symbol, oldPosition) -> new Position(symbol, oldPosition.getUnits() + transaction.getUnitsAdjustment()));
    }

    public static class Builder {
        private Map<String, Position> positions;
        private TradeClock clock;
        private MarketDao marketDao;

        public Builder() {
            this.positions = new HashMap<>();
            this.clock = null;
            this.marketDao = null;
        }

        public Builder withPosition(Position position) {
            positions.put(position.getSymbol(), position);
            return this;
        }

        public Builder withTradeClock(TradeClock clock) {
            this.clock = clock;
            return this;
        }

        public Builder withMarketDao(MarketDao marketDao) {
            this.marketDao = marketDao;
            return this;
        }

        public Portfolio build() {
            Preconditions.checkNotNull(clock);
            Preconditions.checkNotNull(marketDao);
            return new Portfolio(positions, clock, marketDao);
        }
    }
}
