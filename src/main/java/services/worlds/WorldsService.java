package services.worlds;

import abstracts.Singleton;
import apis.guildStats.GuildStats;
import apis.guildStats.models.ChangeName;
import apis.guildStats.models.ChangeWorld;
import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import apis.tibiaTrade.TibiaTradeAPI;
import apis.tibiaTrade.model.world.TibiaTradeWorld;
import apis.tibiaTrade.model.world.TibiaTradeWorldsModel;
import lombok.extern.slf4j.Slf4j;
import services.worlds.enums.Status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
public final class WorldsService extends Singleton {
    private WorldModel worldsData;
    private TibiaTradeWorldsModel worldsCache;
    private final TibiaDataAPI tibiaDataAPI;
    private final TibiaTradeAPI tibiaTradeAPI;
    private final GuildStats guildStats;

    private WorldsService() {
        tibiaDataAPI = new TibiaDataAPI();
        tibiaTradeAPI = new TibiaTradeAPI();
        guildStats = new GuildStats();
        refreshCache();
    }

    public static WorldsService getInstance() {
        return getInstance(WorldsService.class);
    }

    private void refreshCache() {
        new Thread(() -> {
            while(true) {
                try {
                    log.info("Caching worlds data");
                    worldsData = null;
                    getWorlds();
                    synchronized (this) {
                        wait(120000);
                    }
                } catch (Exception e) {
                    log.info("Could not get worlds data - {}", e.getMessage());
                }
            }
        }).start();

        //resets tibia trade cache
        new Thread(() -> {
            while(true) {
                try {
                    synchronized (this) {
                        wait(21600000);
                    }
                    log.info("Resetting tibia trade cache");
                    worldsCache = null;
                } catch (Exception ignore) {
                }
            }
        }).start();
    }

    public WorldModel getWorlds() {
        if(worldsData != null) return worldsData;

        try {
            worldsData = tibiaDataAPI.getWorlds();
            setWorldsIds();
        } catch (Exception ignore) {
        }

        return worldsData;
    }

    public WorldData getWorldData(String world) {
        Optional<WorldData> worldData = worldsData.getWorlds()
                .getRegular_worlds()
                .stream().filter(x -> x.getName().equalsIgnoreCase(world))
                .findFirst();
        return worldData.orElse(null);
    }

    public WorldModel getServerSaveWorlds() {
        WorldModel worlds = tibiaDataAPI.getWorlds();
        worlds.getWorlds().setPlayers_online("0");
        worlds.getWorlds().getRegular_worlds().forEach(x -> {
            x.setStatus(Status.OFFLINE.name());
            x.setPlayers_online(0);
        });
        return worlds;
    }

    public List<ChangeName> getChangedNames(String world) {
        List<ChangeName> changedList = this.guildStats.getChangedNames(world);
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return changedList.stream().filter(x -> x.getChangeDate().equals(currentDate)).toList();
    }

    public List<ChangeWorld> getChangedWorld(String world) {
        List<ChangeWorld> changeWorldList = this.guildStats.getTransferFromWorld(world);
        changeWorldList.addAll(this.guildStats.getTransferToWorld(world));
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return changeWorldList.stream().filter(x -> x.getChangeDate().equals(currentDate)).toList();
    }

    private void setWorldsIds() {
        for(WorldData data : worldsData.getWorlds().getRegular_worlds()) {
            data.setId(getTibiaTradeWorldId(data.getName()));
        }
    }

    private Integer getTibiaTradeWorldId(String worldName) {
        if(worldsCache != null) return findWorld(worldName);
        TibiaTradeWorldsModel model = tibiaTradeAPI.getWorlds();

        if(model.getWorlds().isEmpty()) {
            log.info("Could not get worlds data");
            return 0;
        }

        worldsCache = model;
        return findWorld(worldName);
    }

    private Integer findWorld(String worldName) {
        Optional<TibiaTradeWorld> world = worldsCache.getWorlds()
                .stream()
                .filter(x -> x.getName().equalsIgnoreCase(worldName))
                .findFirst();

        if(world.isPresent()) return world.get().getId();
        log.info("Could not find world {}", worldName);
        return 0;
    }
}
