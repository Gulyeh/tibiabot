package apis.guildStats;

import apis.WebClient;
import apis.guildStats.models.ChangeName;
import apis.guildStats.models.ChangeWorld;
import apis.tibiaData.enums.Vocation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class GuildStats extends WebClient {
    @Override
    protected String getUrl() {
        return "https://guildstats.eu/";
    }

    private String changedNames(String world) {
        return getUrl() + "changed-names/" + world;
    }

    private String changedWorldsFrom(String fromWorld) {
        return getUrl() + "world-transfer?worldFormer=" + fromWorld;
    }

    private String changedWorldsTo(String toWorld) {
        return getUrl() + "world-transfer?worldActual=" + toWorld;
    }

    public List<ChangeName> getChangedNames(String world) {
        try {
            List<ChangeName> changedNames = new ArrayList<>();
            String response = sendRequestUsingFlareSolver(changedNames(world)).getSolution().getResponse();

            Element table = Jsoup.parse(response).getElementById("namesTable");
            if (table == null) return changedNames;

            Element tbody = table.selectFirst("tbody");
            if (tbody == null) return changedNames;

            for (Element row : tbody.children()) {
                Elements cells = row.children();
                if (cells.size() < 7) continue;

                String actualName = cells.get(1).selectFirst("a").text().trim();
                String previousName = cells.get(2).text().trim();
                Vocation voc = parseVocation(cells.get(3));
                int changedAtLevel = parseLevel(cells.get(5).text());
                String changeDate = cells.get(6).text().trim();

                changedNames.add(new ChangeName(actualName, previousName, voc, changedAtLevel, changeDate));
            }
            return changedNames;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<ChangeWorld> getTransferFromWorld(String fromWorld) {
        try {
            String response = sendRequestUsingFlareSolver(changedWorldsFrom(fromWorld)).getSolution().getResponse();
            return processWorlds(response);
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    public List<ChangeWorld> getTransferToWorld(String toWorld) {
        try {
            String response = sendRequestUsingFlareSolver(changedWorldsTo(toWorld)).getSolution().getResponse();
            return processWorlds(response);
        } catch (Exception ignore) {
            return new ArrayList<>();
        }
    }

    private List<ChangeWorld> processWorlds(String response) {
        List<ChangeWorld> changedWorlds = new ArrayList<>();

        Element table = Jsoup.parse(response).getElementById("transferTable");
        if (table == null) return changedWorlds;

        Element tbody = table.selectFirst("tbody");
        if (tbody == null) return changedWorlds;

        for (Element row : tbody.children()) {
            Elements cells = row.children(); // td elements
            if (cells.size() < 7) continue;

            String name = cells.get(1).selectFirst("a").text().trim();
            int changedAtLevel = parseLevel(cells.get(2).text());
            Vocation voc = parseVocation(cells.get(3));
            String previousWorld = cells.get(4).text().trim();
            String currentWorld = cells.get(5).text().trim();
            String changeDate = cells.get(6).text().trim();

            changedWorlds.add(new ChangeWorld(name, changedAtLevel, voc, previousWorld, currentWorld, changeDate));
        }
        return changedWorlds;
    }

    private int parseLevel(String raw) {
        return Integer.parseInt(raw.replace(",", "").trim());
    }

    private Vocation parseVocation(Element cell) {
        Element span = cell.selectFirst("span.cursor-help");
        String code = (span != null ? span.ownText() : cell.text()).trim();
        return Vocation.valueOf(code);
    }
}
