package apis.tibiaData.model.house;

import lombok.Getter;

@Getter
public class HouseData {
    private String name;
    private int house_id;
    private int size;
    private int rent;
    private boolean rented;
    private boolean auctioned;
    private AuctionData auction;
}
