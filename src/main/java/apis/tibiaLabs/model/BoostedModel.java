package apis.tibiaLabs.model;

import lombok.Getter;
import lombok.Setter;

import static utils.TibiaWiki.formatWikiGifLink;
import static utils.TibiaWiki.formatWikiLink;

@Setter
@Getter
public class BoostedModel {
    private String name;
    private String boostedTypeText;

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
