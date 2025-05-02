package utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static cache.utils.UtilsCache.wikiArticlesLinksMap;
import static cache.utils.UtilsCache.wikiGifLinksMap;
import static utils.Methods.readInputStream;

public final class TibiaWiki {
    private TibiaWiki() {}

    public static String formatWikiGifLink(String name) {
        try {
            name = validateMonsterName(name);
            if(wikiGifLinksMap.containsKey(name)) return wikiGifLinksMap.get(name);

            String query = URLEncoder.encode(name + " gif", StandardCharsets.UTF_8);
            String url = "https://tibia.fandom.com/wiki/Special:Search?scope=internal&query=" + query + "&ns%5B0%5D=6&filter=imageOnly";
            Document document = getWikiPageDocument(url);
            Elements results = document.select("a.unified-search__result__link");

            String fileName = extractBestMatch(results, name);
            if (fileName.isEmpty()) return "";

            String link = "https://tibia.fandom.com/wiki/Special:Redirect/file/" + fileName;
            wikiGifLinksMap.put(name, link);
            return link;
        } catch (Exception ignore) {
            return "";
        }
    }

    public static String formatWikiLink(String name) {
        try {
            name = validateMonsterName(name);
            if(wikiArticlesLinksMap.containsKey(name)) return wikiArticlesLinksMap.get(name);

            String query = URLEncoder.encode(name, StandardCharsets.UTF_8);
            String url = "https://tibia.fandom.com/wiki/Special:Search?scope=internal&navigationSearch=true&query=" + query;
            Document document = getWikiPageDocument(url);
            Elements results = document.select("a.unified-search__result__link");

            String title = extractBestMatchTitle(results, name);
            if (title.isEmpty()) return "";

            wikiArticlesLinksMap.put(name, title);
            return title;
        } catch (Exception ignore) {
            return "";
        }
    }

    public static String getPlayerIcon() {
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
        }};

        if(replaceable.containsKey(name.toLowerCase())) name = replaceable.get(name.toLowerCase());
        return name;
    }

    private static Document getWikiPageDocument(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url)
                .toURL()
                .openConnection();
        conn.setRequestMethod("GET");
        conn.disconnect();
        String content = readInputStream(conn.getInputStream());
        return Jsoup.parse(content);
    }

    private static String extractBestMatch(Elements elements, String name) {
        String output = "";
        for(Element element : elements) {
            String title = element.attr("data-title").toLowerCase();
            if(title.contains("soul core") || !title.contains(name.toLowerCase())) continue;
            output = element.attr("href").split("File:")[1];
            break;
        }

        if (output.isEmpty())
            output = elements.first().attr("href").split("File:")[1].trim();

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
