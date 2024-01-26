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
}
