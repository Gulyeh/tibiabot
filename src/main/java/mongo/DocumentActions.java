package mongo;

import com.google.gson.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static mongo.MongoConnector.mongoDatabase;
import static utils.Configurator.config;

public final class DocumentActions {
    private final static Logger logINFO = LoggerFactory.getLogger(DocumentActions.class);
    private final static String collectionName = config.get("DB_COLLECTION");
    private final static String id = "_id";

    public static <T> List<T> getDocuments(Class<T> classType) {
        MongoCollection<Document> collection = getCollection();
        List<T> list = new ArrayList<>();
        Gson gson = getGson();

        try {
            for (Document document : collection.find()) {
                T model = gson.fromJson(document.toJson(), classType);
                list.add(model);
            }
        } catch (Exception ignore) {
            logINFO.info("Could not get data from database");
        }

        return list;
    }

    public static <T> T getDocument(Snowflake guildId, Class<T> classType) {
        MongoCollection<Document> collection = getCollection();
        Gson gson = getGson();

        Document doc = collection.find(Filters.eq(Filters.gt("guildId", guildId.asString()))).explain();
        if(doc.isEmpty()) {
            logINFO.info("Could not find specified document");
            return null;
        }

        return gson.fromJson(doc.toJson(), classType);
    }

    public static void insertDocuments(Document... document) {
        try {
            MongoCollection<Document> collection = getCollection();
            if (document.length == 1) collection.insertOne(document[0]);
            else collection.insertMany(List.of(document));
            logINFO.info("Inserted data to db");
        } catch (Exception e) {
            logINFO.info("Could not insert data to database: " + e.getMessage());
        }
    }

    public static void deleteDocument(Document... documents) {
        try {
            MongoCollection<Document> collection = getCollection();

            for (Document doc : documents) {
                Bson query = eq(id, doc.get(id));
                collection.deleteOne(query);
            }

            logINFO.info("Removed data from db");
        } catch (Exception e) {
            logINFO.info("Could not delete data from db: " + e.getMessage());
        }
    }

    public static void replaceDocument(Document document) {
        try {
            MongoCollection<Document> collection = getCollection();
            Bson query = eq(id, document.get(id));
            ReplaceOptions opts = new ReplaceOptions().upsert(false);
            collection.replaceOne(query, document, opts);
            logINFO.info("Updated data in db");
        } catch (Exception e) {
            logINFO.info("Could not update data in db: " + e.getMessage());
        }
    }

    private static MongoCollection<Document> getCollection() {
        return mongoDatabase.getCollection(collectionName);
    }

    private static Gson getGson() {
        JsonDeserializer<ObjectId> dec = (jsonElement, type, jsonDeserializationContext) -> new ObjectId(jsonElement.getAsJsonObject().get("$oid").getAsString());
        return new GsonBuilder().registerTypeAdapter(ObjectId.class, dec).create();
    }
}
