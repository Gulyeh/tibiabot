package services.miniWorldEvents.models;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MiniWorldEvent {
    private String mini_world_change_name;
    private Integer mini_world_change_id;
    private String external_url;
    private String mini_world_change_icon;
    private final transient LocalDateTime activationDate = LocalDateTime.now();

    public String getMini_world_change_icon() {
        return "https://tibiatrade.gg/images/mini-world-change/"+mini_world_change_id+".gif";
    }
}
