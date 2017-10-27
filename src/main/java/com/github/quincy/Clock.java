package com.github.quincy;

import java.time.LocalDateTime;

public class Clock implements TradeClock {
    @Override
    public boolean isMarketOpen() {
        return LocalDateTime.now().getHour() >= 17;
    }
}
