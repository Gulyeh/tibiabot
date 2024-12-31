package apis.tibiaData.model.houses;

import lombok.Getter;
import lombok.Setter;

@Getter
public class AuctionData {
    private int current_bid;
    @Setter
    private transient String currentBidder;
    @Setter
    private transient String auctionInfo;
    private String time_left;

    public String getTime_left() {
        if(time_left.isEmpty()) return "No data";
        return time_left;
    }

    public String getCurrentBidder() {
        if(currentBidder.isEmpty()) return "None";
        return currentBidder;
    }
}
