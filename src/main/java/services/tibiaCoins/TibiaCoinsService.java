package services.tibiaCoins;

import services.WebClient;
import services.tibiaCoins.models.PriceModel;

import java.net.http.HttpResponse;

public class TibiaCoinsService extends WebClient {
    public TibiaCoinsService() {

    }

    @Override
    protected String getUrl() {
        return "https://tibiatrade.gg/api/tibiaCoinPrices";
    }

    public PriceModel getPrices() {
        String response = sendRequest(getRequest());
        return getModel(response, PriceModel.class);
    }
}
