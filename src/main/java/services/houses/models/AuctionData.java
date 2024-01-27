package services.houses.models;

import lombok.Getter;

@Getter
public class AuctionData {
    private int current_bid;
    private String time_left;

    public String getTime_left() {
        if(time_left.isEmpty()) return "No data";
        return time_left;
    }
}
