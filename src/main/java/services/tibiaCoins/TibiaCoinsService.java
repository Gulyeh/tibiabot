package services.tibiaCoins;

import apis.tibiaData.model.worlds.WorldData;
import apis.tibiaData.model.worlds.WorldModel;
import apis.tibiaTrade.TibiaTradeAPI;
import apis.tibiaTrade.model.prices.PriceModel;
import apis.tibiaTrade.model.prices.Prices;
import services.worlds.WorldsService;

import java.util.Optional;

public class TibiaCoinsService {
    private final WorldsService worldsService;
    private final TibiaTradeAPI tibiaTradeAPI;

    public TibiaCoinsService(WorldsService worldsService) {
        this.worldsService = worldsService;
        tibiaTradeAPI = new TibiaTradeAPI();
    }

    public PriceModel getPrices() {
        WorldModel worlds = worldsService.getWorlds();
        PriceModel prices = tibiaTradeAPI.getTibiaCoinsPrices();

        for(Prices price : prices.getPrices()) {
           Optional<WorldData> data = worlds.getWorlds().getRegular_worlds()
                   .stream()
                   .filter(x -> x.getName().equalsIgnoreCase(price.getWorld_name()))
                   .findFirst();

           data.ifPresent(price::setWorld);
        }

        return prices;
    }
}
