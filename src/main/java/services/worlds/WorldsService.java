package services.worlds;

import services.interfaces.Cacheable;
import services.WebClient;
import services.worlds.models.WorldData;
import services.worlds.models.WorldModel;

public class WorldsService extends WebClient implements Cacheable {
    private WorldModel worldsData;
    private final TibiaTradeWorldsService tibiaTradeWorldsService;

    public WorldsService() {
        tibiaTradeWorldsService = new TibiaTradeWorldsService();
        clearCache();
    }

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/worlds";
    }

    public WorldModel getWorlds() {
        if(worldsData != null) return worldsData;

        try {
            String response = sendRequest(getRequest());
            worldsData = getModel(response, WorldModel.class);
            setWorldsIds();
            return worldsData;
        } catch (Exception e) {
            logINFO.warn(e.getMessage());
        }

        return null;
    }

    @Override
    public void clearCache() {
        worldsData = null;
        tibiaTradeWorldsService.clearCache();
    }

    private void setWorldsIds() {
        for(WorldData data : worldsData.getWorlds().getRegular_worlds()) {
            data.setId(tibiaTradeWorldsService.getTibiaTradeWorldId(data.getName()));
        }
    }
}
