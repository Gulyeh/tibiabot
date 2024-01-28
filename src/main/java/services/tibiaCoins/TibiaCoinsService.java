package services.tibiaCoins;

import services.WebClient;
import services.tibiaCoins.models.PriceModel;
import services.tibiaCoins.models.Prices;
import services.worlds.WorldsService;
import services.worlds.models.WorldData;
import services.worlds.models.WorldModel;

import java.net.http.HttpResponse;

public class TibiaCoinsService extends WebClient {

    private final WorldsService worldsService;

    public TibiaCoinsService(WorldsService worldsService) {
        this.worldsService = worldsService;
    }

    @Override
    protected String getUrl() {
        return "https://tibiatrade.gg/api/tibiaCoinPrices";
    }

    public PriceModel getPrices() {
        String response = sendRequest(getRequest());
        WorldModel worlds = worldsService.getWorlds();
        PriceModel prices = getModel(response, PriceModel.class);

        for(Prices price : prices.getPrices()) {
           WorldData data = worlds.getWorlds().getRegular_worlds()
                   .stream()
                   .filter(x -> x.getName().equalsIgnoreCase(price.getWorld_name()))
                   .findFirst()
                   .get();

           price.setLocation(data.getLocation_type());
           price.setWorld_type(data.getPvp_type());
           price.setBattleEye_type(data.getBattleEyeType());
        }

        return prices;
    }
}
