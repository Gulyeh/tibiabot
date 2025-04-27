package services.boosteds;

import apis.tibiaOfficial.TibiaOfficial;
import apis.tibiaOfficial.models.BoostedModel;
import interfaces.Cacheable;
import services.boosteds.enums.Boosteds;

import java.util.concurrent.ConcurrentHashMap;

public class BoostedsService implements Cacheable {
    private final ConcurrentHashMap<Boosteds, BoostedModel> boostedCache = new ConcurrentHashMap<>();
    private final TibiaOfficial api;

    public BoostedsService() {
        api = new TibiaOfficial();
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
