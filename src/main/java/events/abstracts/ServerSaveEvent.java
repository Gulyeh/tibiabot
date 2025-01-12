package events.abstracts;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ServerSaveEvent extends EmbeddableEvent {
    private LocalDateTime serverSaveTime;

    public ServerSaveEvent() {
        serverSaveTime = LocalDateTime.now()
                .withHour(10)
                .withMinute(3)
                .withSecond(0);
    }

    protected long getWaitTime(int specifiedMillis) {
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(serverSaveTime) || now.isEqual(serverSaveTime)) serverSaveTime = serverSaveTime.plusDays(1);

        long timeLeft = now.until(serverSaveTime, ChronoUnit.MILLIS);
        if(timeLeft < specifiedMillis) {
            log.info(TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) +
                    " minutes left to server save! Initial specified time to wait was: " + TimeUnit.of(ChronoUnit.MILLIS).toMinutes(specifiedMillis) + "min");
        }

        return timeLeft <= specifiedMillis ? timeLeft : specifiedMillis;
    }

    protected long getWaitTime() {
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(serverSaveTime) || now.isEqual(serverSaveTime)) serverSaveTime = serverSaveTime.plusDays(1);
        long timeLeft = now.until(serverSaveTime, ChronoUnit.MILLIS);
        log.info(TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) + " minutes left to server save!");
        return timeLeft;
    }

    protected boolean isAfterSaverSave() {
        LocalDateTime now = LocalDateTime.now();
        boolean isAfterSS = now.isAfter(serverSaveTime) || now.isEqual(serverSaveTime);
        if(isAfterSS) serverSaveTime = serverSaveTime.plusDays(1);
        return isAfterSS;
    }

    protected boolean isAfterSaverSave(LocalDateTime date) {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(date) || now.isEqual(date);
    }
}
