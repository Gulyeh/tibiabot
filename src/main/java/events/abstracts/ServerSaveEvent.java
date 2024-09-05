package events.abstracts;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public abstract class ServerSaveEvent extends EmbeddableEvent {

    private LocalDateTime serverSaveTime;
    private final int expectedHour = 10;
    private final int expectedMinute = 3;

    public ServerSaveEvent() {
        serverSaveTime = LocalDateTime.now()
                .withHour(expectedHour)
                .withMinute(expectedMinute)
                .withSecond(0);
    }

    protected long getWaitTime(int specifiedMillis) {
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(serverSaveTime) || now.isEqual(serverSaveTime)) serverSaveTime = serverSaveTime.plusDays(1);

        long timeLeft = now.until(serverSaveTime, ChronoUnit.MILLIS);
        if(timeLeft < specifiedMillis) {
            logINFO.info(TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) +
                    " minutes left to server save! Initial specified time to wait was: " + TimeUnit.of(ChronoUnit.MILLIS).toMinutes(specifiedMillis) + "min");
        }

        return timeLeft <= specifiedMillis ? timeLeft : specifiedMillis;
    }

    protected long getWaitTime() {
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(serverSaveTime) || now.isEqual(serverSaveTime)) serverSaveTime = serverSaveTime.plusDays(1);
        long timeLeft = now.until(serverSaveTime, ChronoUnit.MILLIS);
        logINFO.info(TimeUnit.of(ChronoUnit.MILLIS).toMinutes(timeLeft) + " minutes left to server save!");
        return timeLeft;
    }

    protected boolean isAfterSaverSave() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(serverSaveTime) || now.isEqual(serverSaveTime);
    }
}
