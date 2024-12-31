package apis.tibiaTrade.model.prices;

import lombok.Getter;
import lombok.Setter;
import apis.tibiaData.model.worlds.WorldData;

import java.time.*;

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
    private WorldData world = new WorldData();
    private String created_at;

    public String getCreated_at() {
        LocalDateTime instanted = OffsetDateTime.parse(created_at).toLocalDateTime();
        return getFormattedDate(instanted);
    }
}
