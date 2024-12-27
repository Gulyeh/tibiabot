package services.boosteds.models;

import lombok.Getter;
import lombok.Setter;

import static utils.Methods.formatWikiGifLink;
import static utils.Methods.formatWikiLink;

@Getter
public class BoostedModel {
    @Setter
    private String name;
    @Setter
    private String boostedTypeText;
    private String icon_link;
    private String boosted_data_link;

    public String getIcon_link() {
        return formatWikiGifLink(getName());
    }

    public String getBoosted_data_link() {
        return formatWikiLink(getName());
    }

    public String getBoostedTypeText() {
        return boostedTypeText + ":";
    }
}
