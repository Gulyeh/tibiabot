package utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static cache.utils.UtilsCache.*;

public final class Methods {
    public static <K, V> K getKey(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String getFormattedDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm"));
    }

    public static LocalDateTime formatToOffsetTime(String date) {
        Instant utcInstant = Instant.parse(date);
        ZoneId warsawTimeZone = ZoneId.of("Europe/Warsaw");
        ZonedDateTime warsawTime = utcInstant.atZone(ZoneOffset.UTC).withZoneSameInstant(warsawTimeZone);
        return warsawTime.toLocalDateTime();
    }

    public static String formatWikiGifLink(String name) {
        try {
            name = validateMonsterName(name);
            if(wikiGifLinksMap.containsKey(name)) return wikiGifLinksMap.get(name);

            String query = URLEncoder.encode(name + " gif", StandardCharsets.UTF_8);
            Document document = getWikiPageDocument("https://tibia.fandom.com/wiki/Special:Search?scope=internal&query=" + query + "&ns%5B0%5D=6&filter=imageOnly");
            String monsterName = document.select("a.unified-search__result__link").first().attr("href").split("File:")[1];
            String link = "https://tibia.fandom.com/wiki/Special%3ARedirect/file/" + monsterName;

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
            Document document = getWikiPageDocument("https://tibia.fandom.com/wiki/Special:Search?scope=internal&navigationSearch=true&query="+query);
            String link = document.select("a.unified-search__result__link").first().text().trim();

            wikiArticlesLinksMap.put(name, link);
            return link;
        } catch (Exception ignore) {
            return "";
        }
    }

    public static String getPlayerIcon() {
        return formatWikiGifLink("Red Skull Item");
    }

    public static String formatToDiscordLink(String title, String link) {
        return "[" + title + "](" + link + ")";
    }

    private static String validateMonsterName(String name) {
        if(name.equalsIgnoreCase("dragon pack")) name = "Despor";
        return name;
    }

    private static String readInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder content = new StringBuilder();
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            content.append(inputLine).append("\n");
        }
        reader.close();
        return content.toString();
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
}
