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
        this.timerHandler = new TimerHandler(LocalDateTime.now()
                .withHour(10)
                .withMinute(3)
                .withSecond(0), eventName);
        this.worldsService = WorldsService.getInstance();
        this.eventName = eventName;
    }

    /**
     * Returns @defaultWaitTime unless server save time is earlier than specified millis
     */
    public long getTimeAdjustedToServerSave(int defaultWaitTime) {
        long serverSaveTimer = isServerSaveTimer();
        if(serverSaveTimer > 0) return serverSaveTimer;

        return timerHandler.getWaitTime(defaultWaitTime);
    }

    public long getTimeUntilServerSave() {
        long serverSaveTimer = isServerSaveTimer();
        if(serverSaveTimer > 0) return serverSaveTimer;

        return timerHandler.getWaitTimeUntilTimer();
    }

    /**
     * Relies on 'getTimeAdjustedToServerSave'
     */
    public boolean checkAfterSaverSave() {
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


    private long isServerSaveTimer() {
        int serverSaveWaiting = 60000;

        if (isServerOffline()) {
            log.info("[{}] Servers are offline. Waiting for them to go back online - Forced wait time to {} seconds", eventName, serverSaveWaiting / 1000);
            return serverSaveWaiting;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(timerHandler.getTimer()) || now.isEqual(timerHandler.getTimer())) {
            log.info("{} timer has finished. Forced waiting time to {} seconds", eventName, serverSaveWaiting / 1000);
            timerHandler.adjustTimerByDays(1);
            return serverSaveWaiting;
        }

        return 0;
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
