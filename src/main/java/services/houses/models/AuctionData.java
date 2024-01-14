package services.houses.models;

import lombok.Getter;

public class AuctionData {
    @Getter
    private int current_bid;
    @Getter
    private String time_left;
}
