package services.tibiaCoins.models;

import lombok.Getter;
import lombok.Setter;
import services.worlds.enums.BattleEyeType;
import services.worlds.enums.Location;

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
    @Getter
    @Setter
    private String world_type = "";
    @Getter
    @Setter
    private BattleEyeType battleEye_type = BattleEyeType.OFF;
    @Getter
    @Setter
    private Location location = Location.EUROPE;
    private String created_at;

    public String getCreated_at() {
        LocalDateTime instanted = LocalDateTime.ofInstant(Instant.parse(created_at), ZoneOffset.UTC);
        return getFormattedDate(instanted);
    }
}
