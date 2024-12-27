package mongo;

import com.google.gson.*;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import discord4j.common.util.Snowflake;
import mongo.models.GuildModel;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Configurator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static mongo.MongoConnector.mongoDatabase;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static utils.Configurator.config;

public final class DocumentActions {
    private final static Logger logINFO = LoggerFactory.getLogger(DocumentActions.class);
    private final static String collectionName = config.get(Configurator.ConfigPaths.DB_COLLECTION.getName());
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

    public static Document getDocument(Snowflake guildId) {
        MongoCollection<Document> collection = getCollection();

        Document doc = collection.find(Filters.eq("guildId", guildId.asString())).first();
        if(doc == null || doc.isEmpty()) {
            logINFO.info("Could not find specified document");
            return null;
        }

        return doc;
    }

    public static <T> T getDocument(Snowflake guildId, Class<T> classType) {
        MongoCollection<Document> collection = getCollection();
        Gson gson = getGson();

        Document doc = collection.find(Filters.eq("guildId", guildId.asString())).first();
        if(doc == null || doc.isEmpty()) {
            logINFO.info("Could not find specified document");
            return null;
        }

        return gson.fromJson(doc.toJson(), classType);
    }

    public static boolean insertDocuments(Document... document) {
        try {
            MongoCollection<Document> collection = getCollection();
            if (document.length == 1) collection.insertOne(document[0]);
            else collection.insertMany(List.of(document));
            logINFO.info("Inserted data to db");
            return true;
        } catch (Exception e) {
            logINFO.info("Could not insert data to database: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteDocument(Document... documents) {
        try {
            MongoCollection<Document> collection = getCollection();

            for (Document doc : documents) {
                Bson query = eq(id, doc.get(id));
                collection.deleteOne(query);
            }

            logINFO.info("Removed data from db");

            return true;
        } catch (Exception e) {
            logINFO.info("Could not delete data from db: " + e.getMessage());
            return false;
        }
    }

    public static boolean replaceDocument(Document document) {
        try {
            MongoCollection<Document> collection = getCollection();
            Bson query = eq(id, document.get(id));
            collection.replaceOne(query, document);
            logINFO.info("Updated data in db");
            return true;
        } catch (Exception e) {
            logINFO.info("Could not update data in db: " + e.getMessage());
            return false;
        }
    }

    public static Document createDocument(GuildModel model) {

        Document doc = new Document()
                .append("guildId", model.getGuildId())
                .append("world", model.getWorld())
                .append("minimumDeathLevel", model.getDeathMinimumLevel())
                .append("channels", model.getChannels());

        if(model.get_id() != null) doc.append("_id", model.get_id());

        return doc;
    }

    private static MongoCollection<Document> getCollection() {
        var providers = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder()
                        .automatic(true)
                        .build()));
        return mongoDatabase.getCollection(collectionName).withCodecRegistry(providers);
    }

    private static Gson getGson() {
        JsonDeserializer<ObjectId> dec = (jsonElement, type, jsonDeserializationContext) -> new ObjectId(jsonElement.getAsJsonObject().get("$oid").getAsString());
        return new GsonBuilder().registerTypeAdapter(ObjectId.class, dec).create();
    }
}
