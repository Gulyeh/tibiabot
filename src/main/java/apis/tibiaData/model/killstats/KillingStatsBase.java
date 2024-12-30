package apis.tibiaData.model.killstats;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Character.isUpperCase;

@Getter
public class KillingStatsBase {

    private KillingStatsModel killstatistics;

    public KillingStatsBase filterBosses() {
        List<KillingStatsData> stats = new ArrayList<>(killstatistics.getEntries().stream()
                .filter(x -> isUpperCase(x.getRace().charAt(0)))
                .toList());

        killstatistics.setEntries(stats);
        return this;
    }
}
