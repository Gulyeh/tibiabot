package utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class Date {
    public static String getFormattedDate(LocalDateTime date) {
        String month = date.getMonthValue() < 10 ? "0" + date.getMonthValue() : String.valueOf(date.getMonthValue());
        return date.getDayOfMonth() + "-" + month + "-" + date.getYear() + " " + date.getHour() + ":" + date.getMinute();
    }
}
