package mongo;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import utils.Configurator;

import static utils.Configurator.config;

public final class MongoConnector {
    @Getter
    public static MongoDatabase mongoDatabase;
    private static final String login = config.get(Configurator.ConfigPaths.DB_LOGIN.getName());
    private static final String password = config.get(Configurator.ConfigPaths.DB_PASSWORD.getName());
    private static final String db = config.get(Configurator.ConfigPaths.DB_NAME.getName());

    public static void connect() {
        if (mongoDatabase != null) return;
        String uri = "mongodb://" + login + ":" + password + "@localhost:27017/?directConnection=true&authSource="+db;
        MongoClient mongoClient = MongoClients.create(uri);
        mongoDatabase = mongoClient.getDatabase(db);
    }
}
