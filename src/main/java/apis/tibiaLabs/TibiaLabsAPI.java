package apis.tibiaLabs;

import apis.WebClient;
import apis.tibiaLabs.model.BoostedModel;

public class TibiaLabsAPI extends WebClient {
    @Override
    protected String getUrl() {
        return "https://api.tibialabs.com/v3/boosted/";
    }

    public BoostedModel getBoostedCreature() {
        return getBoostedBase("creature");
    }

    public BoostedModel getBoostedBoss() {
        return getBoostedBase("boss");
    }

    private BoostedModel getBoostedBase(String param) {
        String response = sendRequest(getRequest(param));
        BoostedModel model = new BoostedModel();
        if(response.contains(":")) {
            String[] split = response.split(": ");
            if(split.length > 1)
                model.setName(split[1].trim());
            model.setBoostedTypeText(split[0]);
        }
        return model;
    }
}
