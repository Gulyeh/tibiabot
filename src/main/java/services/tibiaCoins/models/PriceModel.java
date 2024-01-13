package services.tibiaCoins.models;

import java.util.Comparator;
import java.util.List;

public class PriceModel {
    private List<Prices> prices;

    public List<Prices> getPrices() {
        return prices.stream().sorted(Comparator.comparing(Prices::getWorld_name)).toList();
    }
}
