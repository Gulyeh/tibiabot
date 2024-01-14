package services.worlds.models;

import lombok.Getter;

@Getter
public class WorldData {
    private String name;
    private String status;
    private int players_online;
    private String location;
    private String pvp_type;
    private String transfer_type;
}
