package apis.tibiaLabs;

import apis.WebClient;
import apis.tibiaLabs.model.BoostedModel;

public class TibiaLabsAPI extends WebClient {
    @Override
    protected String getUrl() {
        return "https://api.tibialabs.com/v2/";
    }

    public BoostedModel getBoostedCreature() {
        return getBoostedBase("boostedcreature");
    }

    public BoostedModel getBoostedBoss() {
        return getBoostedBase("boostedboss");
    }

    private BoostedModel getBoostedBase(String param) {
        String response = sendRequest(getRequest(param));
        BoostedModel model = new BoostedModel();
        if(response.contains(":")) {
            if(response.split(": ").length > 1)
                model.setName(response.split(": ")[1].trim());
            model.setBoostedTypeText(response.split(": ")[0]);
        }
        return model;
    }
}
