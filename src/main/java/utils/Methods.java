package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class Methods {
    private Methods() {}

    public static String getFormattedDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm"));
    }

    public static String readInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder content = new StringBuilder();
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            content.append(inputLine).append("\n");
        }
        reader.close();
        return content.toString();
    }

    public static LocalDateTime formatToOffsetTime(String date) {
        Instant utcInstant = Instant.parse(date);
        ZoneId warsawTimeZone = ZoneId.of("Europe/Warsaw");
        ZonedDateTime warsawTime = utcInstant.atZone(ZoneOffset.UTC).withZoneSameInstant(warsawTimeZone);
        return warsawTime.toLocalDateTime();
    }

    public static String formatToDiscordLink(String title, String link) {
        return "[" + title + "](" + link + ")";
    }
}
