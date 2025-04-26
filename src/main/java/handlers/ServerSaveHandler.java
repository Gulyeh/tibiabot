package handlers;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import services.worlds.WorldsService;
import java.time.LocalDateTime;

@Slf4j
public class ServerSaveHandler {
    private final WorldsService worldsService;
    private final TimerHandler timerHandler;
    private final String eventName;
    @Getter
    private boolean serverSaveInProgress = false;

    public ServerSaveHandler(String eventName) {
        this(LocalDateTime.now()
                .withHour(10)
                .withMinute(3)
                .withSecond(0), eventName);
    }

    public ServerSaveHandler(LocalDateTime time, String eventName) {
        this.timerHandler = new TimerHandler(time, eventName);
        this.worldsService = WorldsService.getInstance();
        this.eventName = eventName;
    }

    /**
     * Returns @defaultWaitTime unless server save time is earlier than specified millis
     */
    public long getTimeAdjustedToServerSave(int defaultWaitTime) {
        if (isServerOffline()) {
            log.info("Servers are offline. Waiting for them to go back online");
            return defaultWaitTime;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(timerHandler.getTimer()) || now.isEqual(timerHandler.getTimer())) {
            timerHandler.adjustTimerByDays(1);
        }
        return timerHandler.getWaitTime(defaultWaitTime);
    }

    /**
     * Relies on 'getTimeAdjustedToServerSave'
     */
    public boolean isAfterSaverSave() {
        LocalDateTime now = LocalDateTime.now();

        if (isServerOffline()) {
            if (!serverSaveInProgress)
                serverSaveInProgress = true;
            return false;
        }

        boolean isScheduledSaveTime = now.isAfter(timerHandler.getTimer()) || now.isEqual(timerHandler.getTimer());
        if (isScheduledSaveTime && !serverSaveInProgress) {
            serverSaveInProgress = true;
            return false;
        }

        if (serverSaveInProgress && !isServerOffline()) {
            serverSaveInProgress = false;
            return true;
        }

        return false;
    }

    private boolean isServerOffline() {
        try {
            var worlds = worldsService.getWorlds().getWorlds();
            return Integer.parseInt(worlds.getPlayers_online()) < 1;
        } catch (Exception e) {
            log.warn("[{}] Error checking server status: {}", eventName, e.getMessage());
            return false;
        }
    }
}
