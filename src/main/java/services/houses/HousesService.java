package services.houses;

import org.slf4j.Logger;
import services.WebClient;

public class HousesService extends WebClient {
    public HousesService() {

    }

    @Override
    protected String getUrl() {
        return "https://api.tibiadata.com/v4/houses/";
    }
}
