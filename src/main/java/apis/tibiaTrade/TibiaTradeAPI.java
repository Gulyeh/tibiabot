package apis.tibiaTrade;

import apis.WebClient;
import apis.tibiaTrade.model.prices.PriceModel;
import apis.tibiaTrade.model.prices.Prices;
import apis.tibiaTrade.model.world.TibiaTradeWorldsModel;
import services.miniWorldEvents.models.MiniWorldEventsModel;

public class TibiaTradeAPI extends WebClient {
    @Override
    protected String getUrl() {
        return "https://tibiatrade.gg/api/";
    }

    private String getMiniEventsUrl(String worldId) {
        return getUrl() + "miniWorldChange/active/"+worldId;
    }

    private String getWorldUrl() {
        return getUrl() + "world";
    }

    private String getCoinsPricesUrl() {
        return getUrl() + "tibiaCoinPrices";
    }

    public MiniWorldEventsModel getMiniWorldEvents(String worldId) {
        String response = sendRequest(getCustomRequest(getMiniEventsUrl(worldId)));
        MiniWorldEventsModel model = getModel(response, MiniWorldEventsModel.class);
        if(model == null) return new MiniWorldEventsModel();
        return model;
    }

    public TibiaTradeWorldsModel getWorlds() {
        String response = sendRequest(getCustomRequest(getWorldUrl()));
        TibiaTradeWorldsModel model = getModel(response, TibiaTradeWorldsModel.class);
        if(model == null) return new TibiaTradeWorldsModel();
        return model;
    }

    public PriceModel getTibiaCoinsPrices() {
        String response = sendRequest(getCustomRequest(getCoinsPricesUrl()));
        PriceModel prices = getModel(response, PriceModel.class);
        if(prices == null) new PriceModel();
        return prices;
    }
}
