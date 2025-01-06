package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        return date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm"));
    }

    public static String formatWikiGifLink(String name) {
        return "https://tibia.fandom.com/wiki/Special:Redirect/file/" + replaceWikiChars(convertToUpperCaseAfterSpace(name)) + ".gif";
    }

    public static String formatWikiLink(String name) {
        return "https://tibia.fandom.com/wiki/" + replaceWikiChars(convertToUpperCaseAfterSpace(name));
    }

    public static String getPlayerIcon() {
        return "https://static.wikia.nocookie.net/tibia/images/2/27/Red_Skull_%28Item%29.gif/revision/latest/thumbnail/width/64/height/64?cb=20220107212229&path-prefix=en";
    }

    private static String replaceWikiChars(String name) {
        if(name.startsWith("a "))
            name = name.replace("a ", "");
        return name
                .replace(" ", "_")
                .replace(" The ", " the ")
                .replace(" Of ", " of ");
    }

    public static String convertToUpperCaseAfterSpace(String value) {
        String[] words = value.split("(\\w+-)|\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words)
        {
            if(word.length() == 1) {
                result.append(word)
                        .append(" ");
                continue;
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }

        return result.toString().trim();
    }
}
