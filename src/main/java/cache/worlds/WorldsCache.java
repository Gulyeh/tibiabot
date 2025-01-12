package cache.worlds;

import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import lombok.Getter;
import services.worlds.enums.Status;

import java.util.concurrent.ConcurrentHashMap;

public final class WorldsCache {
    @Getter
    private static final ConcurrentHashMap<String, Status> worldsStatus = new ConcurrentHashMap<>();

    public static void setWorldsStatus(WorldModel worlds) {
        for(WorldData world : worlds.getWorlds().getRegular_worlds()) {
            worldsStatus.put(world.getName(), world.getStatus_type());
        }
    }
}
