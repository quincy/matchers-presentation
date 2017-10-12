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
 * This object represents the current total ownership of the user, whereas the Ledger represents the change in ownership over time.
 */
public class Portfolio {
    private final Map<String, Position> positions;
    private final TradeExecutor tradeExecutor;

    public Portfolio(Map<String, Position> positions, TradeExecutor tradeExecutor) {
        this.positions = positions;
        this.tradeExecutor = tradeExecutor;
    }

    public Optional<Position> getPosition(String symbol) {
        return Optional.ofNullable(positions.get(symbol));
    }

    public Set<Position> getPositions() {
        return new HashSet<>(positions.values());
    }

    public Transaction trade(Trade order) throws MarketClosedException {
        Transaction transaction = tradeExecutor.execute(order);
        applyTransaction(transaction);
        return transaction;
    }

    private void applyTransaction(Transaction transaction) {
        positions.compute(transaction.getSymbol(),
                (symbol, oldPosition) -> new Position(symbol, oldPosition.getUnits() + transaction.getUnitsAdjustment()));
    }

    public static class Builder {
        private Map<String, Position> positions;
        private TradeExecutor tradeExecutor;

        public Builder() {
            this.positions = new HashMap<>();
            this.tradeExecutor = null;
        }

        public Builder withPosition(Position position) {
            positions.put(position.getSymbol(), position);
            return this;
        }

        public Builder withTradeExecutor(TradeExecutor tradeExecutor) {
            this.tradeExecutor = tradeExecutor;
            return this;
        }

        public Portfolio build() {
            Preconditions.checkNotNull(tradeExecutor);
            return new Portfolio(positions, tradeExecutor);
        }
    }
}
