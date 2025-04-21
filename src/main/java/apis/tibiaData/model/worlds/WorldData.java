package apis.tibiaData.model.worlds;

import lombok.Getter;
import lombok.Setter;
import services.worlds.enums.BattleEyeType;
import services.worlds.enums.Location;
import services.worlds.enums.Status;


public class WorldData {
    @Getter
    @Setter
    private Integer id;
    @Getter
    private String name;
    @Getter
    @Setter
    private int players_online;
    @Getter
    private String pvp_type;
    @Getter
    private String transfer_type;

    private Boolean battleye_protected;
    @Setter
    private String status;
    private String battleye_date;
    private String location;

    public BattleEyeType getBattleEyeType() {
        if(battleye_date.equalsIgnoreCase("release")) return BattleEyeType.GBE;
        return BattleEyeType.YBE;
    }

    public Status getStatus_type() {
        if(status.equalsIgnoreCase("online")) return Status.ONLINE;
        return Status.OFFLINE;
    }

    public Location getLocation_type() {
        if(location.contains("America")) return Location.AMERICA;
        return Location.EUROPE;
    }
}
