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

    private final static String configName = "config.env";
    public static final Dotenv config = Dotenv
            .configure()
            .ignoreIfMalformed()
            .filename(configName)
            .load();
}
