package com.github.quincy;

import java.time.LocalDateTime;

public class Clock implements TradeClock {
    @Override
    public boolean isMarketOpen() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour() >= 9 && now.getHour() < 17;
    }
}
