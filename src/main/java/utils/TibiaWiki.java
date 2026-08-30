package utils;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import static cache.utils.UtilsCache.wikiArticlesLinksMap;
import static cache.utils.UtilsCache.wikiGifLinksMap;

public final class TibiaWiki {
    private TibiaWiki() {}

    private static final apis.tibiaWiki.TibiaWiki client = new apis.tibiaWiki.TibiaWiki();

    public static byte[] formatWikiGifLink(String name) {
        try {
            name = validateMonsterName(name);
            if(wikiGifLinksMap.containsKey(name)) return wikiGifLinksMap.get(name);

            String query = URLEncoder.encode(name + " gif", StandardCharsets.UTF_8);
            Document document = client.getGif(query);
            Elements results = document.select("a.unified-search__result__link");

            String fileName = extractBestMatch(results, name);
            if (fileName.isEmpty()) return null;

            byte[] gifBytes = client.getGifFile(fileName);
            if(gifBytes != null)
                wikiGifLinksMap.put(name, gifBytes);
            return gifBytes;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static String formatWikiLink(String name) {
        try {
            name = validateMonsterName(name);
            if(wikiArticlesLinksMap.containsKey(name)) return wikiArticlesLinksMap.get(name);

            String query = URLEncoder.encode(name, StandardCharsets.UTF_8);
            Document document =  client.getWiki(query);
            Elements results = document.select("a.unified-search__result__link");

            String title = extractBestMatchTitle(results, name);
            if (title.isEmpty()) return "";

            wikiArticlesLinksMap.put(name, title);
            return title;
        } catch (Exception ignore) {
            return "";
        }
    }

    public static byte[] getWorldChangeIcon() {
        return client.getImageBytes("https://static.wikia.nocookie.net/tibia/images/4/45/World_Transfer.png/revision/latest?cb=20150705090003&path-prefix=en&format=original");
    }

    public static byte[] getNameChangeIcon() {
        return client.getImageBytes("https://static.wikia.nocookie.net/tibia/images/d/d7/Name_Change.png/revision/latest?cb=20150705090002&path-prefix=en&format=original");
    }

    public static byte[] getPlayerIcon() {
        return formatWikiGifLink("Red Skull Item");
    }

    private static String validateMonsterName(String name) {
        Map<String, String> replaceable = new HashMap<>() {{
            put("dragon pack", "Despor");
            put("earth", "Poison Gas");
            put("energy", "Energy Field (Field)");
            put("death", "Death Effect");
            put("agony", "Darkfield");
            put("drowning", "Water Vortex");
            put("sabretooth", "Sabretooth (Creature)");
            put("nomad", "Nomad (Basic)");
            put("adventurers nemesis", "Barrel (Brown)");
            put("ice", "Ice Explosion Effect");
        }};

        if(replaceable.containsKey(name.toLowerCase())) name = replaceable.get(name.toLowerCase());
        return name;
    }

    private static String extractBestMatch(Elements elements, String name) {
        LinkedList<String> listOfMonsters = new LinkedList<>();
        String output = "";
        for(Element element : elements) {
            String title = element.attr("data-title").toLowerCase();
            if(title.contains("soul core")) continue;
            listOfMonsters.add(element.attr("href").split("File:")[1]);
        }

        if(!listOfMonsters.isEmpty()) {
           Optional<String> monster = listOfMonsters.stream().filter(x ->
                   x.equalsIgnoreCase(name.replace(" ", "_") + ".gif")).findFirst();
           output = monster.orElseGet(() -> listOfMonsters.stream().findFirst().get());
        }

        return output;
    }

    private static String extractBestMatchTitle(Elements elements, String name) {
        for (Element el : elements) {
            if (el.attr("data-title").toLowerCase().contains(name.toLowerCase()))
                return el.text().trim();
        }

        return !elements.isEmpty() ? elements.first().text().trim() : "";
    }
}
