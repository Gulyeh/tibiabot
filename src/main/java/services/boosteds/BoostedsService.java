package services.boosteds;

import apis.WebClient;
import apis.tibiaLabs.TibiaLabsAPI;
import services.boosteds.enums.Boosteds;
import apis.tibiaLabs.model.BoostedModel;
import services.interfaces.Cacheable;
import java.util.concurrent.ConcurrentHashMap;

public class BoostedsService extends WebClient implements Cacheable {
    private final ConcurrentHashMap<Boosteds, BoostedModel> boostedCache = new ConcurrentHashMap<>();
    private final TibiaLabsAPI api;

    public BoostedsService() {
        api = new TibiaLabsAPI();
    }

    @Override
    protected String getUrl() {
        return "https://api.tibialabs.com/v2/";
    }

    public BoostedModel getBoostedCreature() {
        if(boostedCache.containsKey(Boosteds.CREATURE)) return boostedCache.get(Boosteds.CREATURE);
        BoostedModel model = api.getBoostedCreature();
        boostedCache.put(Boosteds.CREATURE, model);
        return model;
    }

    public BoostedModel getBoostedBoss() {
        if(boostedCache.containsKey(Boosteds.BOSS)) return boostedCache.get(Boosteds.BOSS);
        BoostedModel model = api.getBoostedBoss();
        boostedCache.put(Boosteds.BOSS, model);
        return model;
    }

    @Override
    public void clearCache() {
        boostedCache.clear();
    }
}
