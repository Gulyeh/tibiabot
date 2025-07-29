package handlers;

import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import services.worlds.WorldsService;
import services.worlds.enums.Status;

import java.time.LocalDateTime;
import java.util.List;

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
            WorldModel worlds = worldsService.getWorlds();
            if(worlds == null) return false;

            List<WorldData> onlines = worlds.getWorlds().getRegular_worlds().stream()
                    .filter(x -> x.getStatus_type().equals(Status.ONLINE) &&
                            !x.getName().equals("Zuna") && !x.getName().equals("Zunera")).toList();

            return Integer.parseInt(worlds.getWorlds().getPlayers_online()) < 20 && onlines.isEmpty();
        } catch (Exception e) {
            log.warn("[{}] Error checking server status: {}", eventName, e.getMessage());
            return false;
        }
    }
}
