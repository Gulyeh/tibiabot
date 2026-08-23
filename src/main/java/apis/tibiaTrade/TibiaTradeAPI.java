package apis.tibiaTrade;

import apis.WebClient;
import apis.tibiaTrade.model.BaseResponseModel;
import apis.tibiaTrade.model.prices.PriceModel;
import apis.tibiaTrade.model.world.TibiaTradeWorldsModel;
import com.google.gson.reflect.TypeToken;
import services.miniWorldEvents.models.MiniWorldEventsModel;

import java.lang.reflect.Type;
import java.util.List;

public class TibiaTradeAPI extends WebClient {
    @Override
    protected String getUrl() {
        return "https://tibiatrade.gg/trpc/";
    }

    private String getMiniEventsUrl(String worldId) {
        return getUrl() + "miniWorldChange.listActive?batch=1&input=%7B\"0\"%3A%7B\"world_id\"%3A"+worldId+"%7D%7D";
    }

    private String getWorldUrl() {
        return getUrl() + "world.list?batch=1";
    }

    private String getCoinsPricesUrl() {
        return getUrl() + "tibiaCoinPrice.list?input=%7B%7D";
    }

    public MiniWorldEventsModel getMiniWorldEvents(String worldId) {
        String response = sendRequest(getCustomRequest(getMiniEventsUrl(worldId)));
        Type type = new TypeToken<BaseResponseModel<MiniWorldEventsModel>>(){}.getType();
        BaseResponseModel<MiniWorldEventsModel> model = getModel(response, type);
        if(model == null) return new MiniWorldEventsModel();
        return model.getResult().getData();
    }

    public TibiaTradeWorldsModel getWorlds() {
        String response = sendRequest(getCustomRequest(getWorldUrl()));
        Type type = new TypeToken<List<BaseResponseModel<TibiaTradeWorldsModel>>>(){}.getType();
        List<BaseResponseModel<TibiaTradeWorldsModel>> model = getModel(response, type);
        if(model == null) return new TibiaTradeWorldsModel();
        return model.get(0).getResult().getData();
    }

    public PriceModel getTibiaCoinsPrices() {
        String response = sendRequest(getCustomRequest(getCoinsPricesUrl()));
        Type type = new TypeToken<BaseResponseModel<PriceModel>>(){}.getType();
        BaseResponseModel<PriceModel> prices = getModel(response, type);
        if(prices == null) return new PriceModel();
        return prices.getResult().getData();
    }
}
