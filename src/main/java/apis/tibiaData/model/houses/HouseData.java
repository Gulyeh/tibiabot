package apis.tibiaData.model.houses;

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

    public String getHouseLink(String world) {
        return "https://www.tibia.com/community/?subtopic=houses&page=view&houseid=" + getHouse_id() + "&world=" + world;
    }
}
