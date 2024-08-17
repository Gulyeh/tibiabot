package events.abstracts;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public abstract class ServerSaveEvent extends EmbeddableEvent {
    protected long getWaitTime(int specifiedMillis) {
        int expectedHour = 10;
        int expectedMinute = 2;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requiredTime = now
                .withHour(expectedHour)
                .withMinute(expectedMinute)
                .withSecond(0);

        if(now.getHour() > expectedHour || (now.getHour() == expectedHour && now.getMinute() > expectedMinute))
            requiredTime = requiredTime.plusDays(1);

        long timeLeft = now.until(requiredTime, ChronoUnit.MILLIS);

        return timeLeft <= specifiedMillis ? timeLeft : specifiedMillis;
    }
}
