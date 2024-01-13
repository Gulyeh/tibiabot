package utils;

import io.github.cdimascio.dotenv.Dotenv;

public final class Configurator {
    public static final Dotenv config = Dotenv
            .configure()
            .ignoreIfMalformed()
            .filename("config.env")
            .load();
}
