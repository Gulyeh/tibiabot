package apis.tibiaData.model.deathtracker;

import lombok.Getter;

import java.beans.Encoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
public class GuildData {
    private String name;
    private String rank;
    public String getGuildLink() {
        return "https://www.tibia.com/community/?subtopic=guilds&page=view&GuildName=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
    }
}
