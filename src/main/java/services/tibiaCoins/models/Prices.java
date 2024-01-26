package services.tibiaCoins.models;

import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static utils.Methods.getFormattedDate;

public class Prices {
    @Getter
    private String world_name;
    @Getter
    private String buy_average_price;
    @Getter
    private String sell_average_price;
    @Getter
    private String buy_highest_price;
    @Getter
    private String sell_lowest_price;
    private String created_at;

    public String getCreated_at() {
        LocalDateTime instanted = LocalDateTime.ofInstant(Instant.parse(created_at), ZoneOffset.UTC);
        return getFormattedDate(instanted);
    }
}
