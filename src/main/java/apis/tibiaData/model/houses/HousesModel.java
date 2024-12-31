package apis.tibiaData.model.houses;

import lombok.Getter;

import java.util.List;

@Getter
public class HousesModel {
    private List<HouseData> house_list;
    private List<HouseData> guildhall_list;
    private String world;
    private String town;

    public List<HouseData> getHouse_list() {
        return house_list.stream().filter(HouseData::isAuctioned).toList();
    }
}
