package apis.tibiaLabs;

import apis.WebClient;
import apis.tibiaLabs.model.BoostedModel;

public class TibiaLabsAPI extends WebClient {
    @Override
    protected String getUrl() {
        return "https://api.tibialabs.com/v2/";
    }

    public BoostedModel getBoostedCreature() {
        String response = sendRequest(getRequest("boostedcreature"));
        BoostedModel model = new BoostedModel();
        if(response.contains(":")) {
            model.setName(response.split(": ")[1].trim());
            model.setBoostedTypeText(response.split(": ")[0]);
        }
        return model;
    }

    public BoostedModel getBoostedBoss() {
        String response = sendRequest(getRequest("boostedboss"));
        BoostedModel model = new BoostedModel();
        if(response.contains(":")) {
            model.setName(response.split(": ")[1].trim());
            model.setBoostedTypeText(response.split(": ")[0]);
        }
        return model;
    }
}
