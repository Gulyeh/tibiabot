package cache;

import lombok.Getter;
import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import services.worlds.enums.Status;

import java.util.HashMap;

public final class UtilsCache {
    @Getter
    private static HashMap<String, Status> worldsStatus = new HashMap<>();

    public static void setWorldsStatus(WorldModel worlds) {
        for(WorldData world : worlds.getWorlds().getRegular_worlds()) {
            worldsStatus.put(world.getName(), world.getStatus_type());
        }
    }
}
