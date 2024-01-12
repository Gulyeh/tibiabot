package services.tibiaCoins.models;

import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Prices {
    @Getter
    private String world_name;
    @Getter
    private String buy_average_price;
    @Getter
    private String sell_average_price;
    private String created_at;

    public String getCreated_at() {
        LocalDateTime instanted = LocalDateTime.ofInstant(Instant.parse(created_at), ZoneOffset.UTC);
        String month = instanted.getMonthValue() < 10 ? "0" + instanted.getMonthValue() : String.valueOf(instanted.getMonthValue());
        return instanted.getDayOfMonth() + "-" + month + "-" + instanted.getYear() + " " + instanted.getHour() + ":" + instanted.getMinute();
    }
}
