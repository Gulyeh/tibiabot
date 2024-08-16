package cache;

import lombok.Getter;
import services.worlds.enums.Status;
import services.worlds.models.WorldData;
import services.worlds.models.WorldModel;

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
