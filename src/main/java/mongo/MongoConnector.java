package mongo;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;

import static utils.Configurator.config;

public final class MongoConnector {
    @Getter
    public static MongoDatabase mongoDatabase;
    private static final String login = config.get("DB_LOGIN");
    private static final String password = config.get("DB_PASSWORD");
    private static final String db = config.get("DB_NAME");

    public static void connect() {
        if (mongoDatabase != null) return;
        String uri = "mongodb://" + login + ":" + password + "@localhost:27017/?directConnection=true&authSource="+db;
        MongoClient mongoClient = MongoClients.create(uri);
        mongoDatabase = mongoClient.getDatabase(db);
    }
}
