package services.worlds;

import org.slf4j.Logger;
import services.WebClient;

public class WorldsService extends WebClient {
    public WorldsService() {

    }

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/worlds";
    }
}
