package utils;

import java.time.LocalDateTime;
import java.util.Map;

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
        String month = date.getMonthValue() < 10 ? "0" + date.getMonthValue() : String.valueOf(date.getMonthValue());
        return date.getDayOfMonth() + "-" + month + "-" + date.getYear() + " " + date.getHour() + ":" + date.getMinute();
    }

    public static String formatWikiGifLink(String name) {
        return "https://tibia.fandom.com/wiki/Special:Redirect/file/" + replaceWikiChars(name) + ".gif";
    }

    public static String formatWikiLink(String name) {
        return "https://tibia.fandom.com/wiki/" + replaceWikiChars(name);
    }

    public static String getNotFoundIcon() {
        return "https://static-00.iconduck.com/assets.00/image-not-found-01-icon-1024x1024-ctusxejn.png";
    }

    private static String replaceWikiChars(String name) {
        return name.replace(" ", "_").replace("The", "the");
    }
}
