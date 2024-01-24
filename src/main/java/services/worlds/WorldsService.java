package services.worlds;

import services.WebClient;
import services.worlds.models.WorldModel;

import java.net.http.HttpResponse;

public class WorldsService extends WebClient {
    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/worlds";
    }

    public WorldModel getWorlds() {
        try {
            String response = sendRequest(getRequest());
            return getModel(response, WorldModel.class);
        } catch (Exception e) {
            logINFO.warn(e.getMessage());
        }

        return null;
    }
}
