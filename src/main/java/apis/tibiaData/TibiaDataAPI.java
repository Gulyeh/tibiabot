package apis.tibiaData;

import apis.WebClient;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import apis.tibiaData.model.house.HouseBase;
import apis.tibiaData.model.house.HousesModel;
import apis.tibiaData.model.charactersOnline.World;
import apis.tibiaData.model.killstats.KillingStatsBase;
import apis.tibiaData.model.killstats.KillingStatsModel;
import apis.tibiaData.model.worlds.WorldModel;
import apis.tibiaData.model.charactersOnline.CharacterData;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TibiaDataAPI extends WebClient {
    private String world;

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/";
    }

    private String getWorldUrl() {
        return getUrl() + "world/" + world;
    }

    private String getUrlCharacter(String charName) {
        return getUrl() + "character/" + URLEncoder.encode(charName, StandardCharsets.UTF_8);
    }

    private String getHousesUrl(String townName) {
        return getUrl() + "houses/" + world + "/" + townName;
    }

    private String getKillStatisticsUrl() {
        return getUrl() + "killstatistics/" + world;
    }

    private String getWorldsUrl() {
        return getUrl() + "worlds";
    }


    public List<CharacterData> getCharactersOnWorld(String world) {
        this.world = world;
        String response = sendRequest(getCustomRequest(getWorldUrl()));
        World model = getModel(response, World.class);
        if(model == null) return List.of();
        return model.getWorld().getOnline_players();
    }

    public CharacterResponse getCharacterData(String charName) {
        String response = sendRequest(getCustomRequest(getUrlCharacter(charName)));
        CharacterResponse model = getModel(response, CharacterResponse.class);
        if(model == null) return new CharacterResponse();
        return model;
    }

    public HousesModel getTownHouses(String world, String townName) {
        this.world = world;
        String response = sendRequest(getCustomRequest(getHousesUrl(townName)));
        HousesModel model = getModel(response, HouseBase.class).getHouses();
        if(model == null) return new HousesModel();
        return model;
    }

    public KillingStatsModel getKillStatistics(String world) {
        this.world = world;
        String response = sendRequest(getCustomRequest(getKillStatisticsUrl()));
        KillingStatsModel model = getModel(response, KillingStatsBase.class).filterBosses().getKillstatistics();
        if(model == null) return new KillingStatsModel();
        return model;
    }

    public WorldModel getWorlds() {
        String response = sendRequest(getCustomRequest(getWorldsUrl()));
        WorldModel worldsData = getModel(response, WorldModel.class);
        if(worldsData == null) return new WorldModel();
        return worldsData;
    }
}
