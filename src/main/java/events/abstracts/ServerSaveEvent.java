package events.abstracts;

import lombok.extern.slf4j.Slf4j;
import services.worlds.WorldsService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ServerSaveEvent extends TimerEvent {
    private final WorldsService worldsService;
    private final long defaultTime = 120000;

    public ServerSaveEvent(WorldsService worldsService) {
        super(LocalDateTime.now()
                .withHour(10)
                .withMinute(3)
                .withSecond(0));
        this.worldsService = worldsService;
    }

    protected long getWaitTime(int specifiedMillis) {
        if(!isServerOnline()) return specifiedMillis;
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(timer) || now.isEqual(timer)) adjustTimerByDays(1);
        return super.getWaitTime(specifiedMillis);
    }

    protected long getWaitTime() {
        if(!isServerOnline()) return defaultTime;
        LocalDateTime now = LocalDateTime.now();
        if(now.isAfter(timer) || now.isEqual(timer)) adjustTimerByDays(1);
        return super.getWaitTime();
    }

    protected boolean isAfterSaverSave() {
        if(!isServerOnline()) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(timer) || now.isEqual(timer);
    }

    private boolean isServerOnline() {
        if(worldsService.getWorlds().getWorlds() == null || Integer.parseInt(worldsService.getWorlds().getWorlds().getPlayers_online()) < 1) {
            log.info("Servers are offline. Waiting for them to go back online");
            return false;
        }

        return true;
    }
}
