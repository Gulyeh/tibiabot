package services.boosteds.models;

import lombok.Getter;
import lombok.Setter;

@Getter
public class BoostedModel {
    @Setter
    private String name;
    @Setter
    private String boostedTypeText;
    private String icon_link;
    private String boosted_data_link;

    public String getIcon_link() {
        return "https://tibia.fandom.com/wiki/Special:Redirect/file/"+getName().replace(" ", "_")+".gif";
    }

    public String getBoosted_data_link() {
        return "https://tibia.fandom.com/wiki/"+getName().replace(" ", "_");
    }

    public String getBoostedTypeText() {
        return boostedTypeText + ":";
    }
}
