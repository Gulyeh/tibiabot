package services.boosteds;

import services.WebClient;
import services.boosteds.enums.Boosteds;
import services.boosteds.models.BoostedModel;
import services.interfaces.Cacheable;

import java.util.HashMap;
import java.util.Map;

public class BoostedsService extends WebClient implements Cacheable {
    private Map<Boosteds, BoostedModel> boostedCache = new HashMap<>();

    @Override
    protected String getUrl() {
        return "https://api.tibialabs.com/v2/";
    }

    public BoostedModel getBoostedCreature() {
        if(boostedCache.containsKey(Boosteds.CREATURE)) return boostedCache.get(Boosteds.CREATURE);

        String response = sendRequest(getRequest("boostedcreature"));
        BoostedModel model = new BoostedModel();
        if(response.contains(":")) {
            model.setName(response.split(": ")[1].trim());
            model.setBoostedTypeText(response.split(": ")[0]);
            boostedCache.put(Boosteds.CREATURE, model);
        }
        return model;
    }

    public BoostedModel getBoostedBoss() {
        if(boostedCache.containsKey(Boosteds.BOSS)) return boostedCache.get(Boosteds.BOSS);

        String response = sendRequest(getRequest("boostedboss"));
        BoostedModel model = new BoostedModel();
        if(response.contains(":")) {
            model.setName(response.split(": ")[1].trim());
            model.setBoostedTypeText(response.split(": ")[0]);
            boostedCache.put(Boosteds.BOSS, model);
        }
        return model;
    }

    @Override
    public void clearCache() {
        boostedCache.clear();
    }
}
