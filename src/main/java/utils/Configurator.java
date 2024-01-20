package utils;

import io.github.cdimascio.dotenv.Dotenv;

public final class Configurator {
    private final static String configName = "config.env";
    public static final Dotenv config = Dotenv
            .configure()
            .ignoreIfMalformed()
            .filename(configName)
            .load();
}
