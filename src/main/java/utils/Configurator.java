package utils;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.AllArgsConstructor;
import lombok.Getter;

public final class Configurator {

    @Getter
    @AllArgsConstructor
    public enum ConfigPaths {
        BOT_KEY("BOT_KEY"),
        DB_LOGIN("DB_LOGIN"),
        DB_PASSWORD("DB_PASSWORD"),
        DB_NAME("DB_NAME"),
        DB_COLLECTION("DB_COLLECTION"),
        CHROMEDRIVER_PATH("CHROMEDRIVER_PATH"),
        EVENTS_PATH("EVENTS_PATH");

        private final String name;
    }

    @Getter
    @AllArgsConstructor
    public enum Emoji {
        COIN_EMOJI("COIN_EMOJI_ID"),
        GREEN_BE("GREEN_BE_ID"),
        YELLOW_BE("YELLOW_BE_ID"),
        BLANK_EMOJI("BLANK_EMOJI_ID"),
        RED_DOWN_EMOJI("RED_DOWN_EMOJI_ID"),
        GREEN_DOWN_EMOJI("GREEN_DOWN_EMOJI_ID"),
        GREEN_UP_EMOJI("GREEN_UP_EMOJI_ID");

        private final String id;
    }

    private final static String configName = "config.env";
    public static final Dotenv config = Dotenv
            .configure()
            .ignoreIfMalformed()
            .filename(configName)
            .load();
}
