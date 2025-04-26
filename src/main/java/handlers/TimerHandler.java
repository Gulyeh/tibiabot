package handlers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimerHandler {
    @Getter
    private LocalDateTime timer;
    private final String eventName;

    public TimerHandler(LocalDateTime timer, String eventName) {
        this.timer = timer;
        if(isAfterTimer()) this.timer = timer.plusDays(1);
        this.eventName = eventName;
    }

    /**
     * Returns @specifiedMillis or adjusted millis to match set timer
     */
    public long getWaitTime(int specifiedMillis) {
        long timeLeft = LocalDateTime.now().until(timer, ChronoUnit.MILLIS);
        if(timeLeft < specifiedMillis) {
            log.info("{} minutes left to {} execution! Initial specified time to wait was: {}min",
                    TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft), eventName, TimeUnit.of(ChronoUnit.MILLIS).toMinutes(specifiedMillis));
        }

        return timeLeft <= specifiedMillis ? timeLeft : specifiedMillis;
    }

    /**
     * Returns wait time until timer time and adjust timer by @adjustTimerDays
     */
    public long getWaitTimeUntilTimer(int adjustTimerDays) {
        long timeLeft = LocalDateTime.now().until(timer, ChronoUnit.MILLIS);
        adjustTimerByDays(adjustTimerDays);
        log.info("{} minutes left to {} execution!", TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft), eventName);
        return timeLeft;
    }

    public boolean isAfterTimer() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(timer) || now.isEqual(timer);
    }

    public void adjustTimerByDays(int days) {
        timer = timer.plusDays(days);
    }
}
