package interfaces;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public interface CharacterLink {
    default String getCharacterLink() {
        return "https://www.tibia.com/community/?name=" + URLEncoder.encode(getName(), StandardCharsets.UTF_8);
    }

    String getName();
}
