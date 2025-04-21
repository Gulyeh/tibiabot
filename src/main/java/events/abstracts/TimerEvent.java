package events.abstracts;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class TimerEvent extends EmbeddableEvent {
    protected LocalDateTime timer;

    /**
     * set when getWaitTime(int specifiedMillis) is used
     */
    protected boolean serverSaveOccurs;

    public TimerEvent(LocalDateTime timer) {
        this.timer = timer;
    }

    protected long getWaitTime(int specifiedMillis) {
        long timeLeft = LocalDateTime.now().until(timer, ChronoUnit.MILLIS);
        serverSaveOccurs = false;
        if(timeLeft < specifiedMillis) {
            serverSaveOccurs = true;
            log.info(TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) +
                    " minutes left to " + getEventName() + " execution! Initial specified time to wait was: " + TimeUnit.of(ChronoUnit.MILLIS).toMinutes(specifiedMillis) + "min");
        }

        return timeLeft <= specifiedMillis ? timeLeft : specifiedMillis;
    }

    protected long getWaitTime() {
        long timeLeft =  LocalDateTime.now().until(timer, ChronoUnit.MILLIS);
        log.info(TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) + " minutes left to " + getEventName() + " execution!");
        return timeLeft;
    }

    protected void adjustTimerByDays(int days) {
        timer = timer.plusDays(days);
    }

    protected boolean isAfterDate(LocalDateTime date) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(date) || now.isEqual(date);
    }
}
