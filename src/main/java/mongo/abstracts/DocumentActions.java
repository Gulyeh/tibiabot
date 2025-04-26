package mongo.abstracts;

import abstracts.Singleton;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import discord.Connector;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import utils.Configurator;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static mongo.MongoConnector.mongoDatabase;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static utils.Configurator.config;

@Slf4j
public abstract class DocumentActions<T> extends Singleton {

    protected String collectionName;
    protected final String id = "_id";

    public DocumentActions(Configurator.ConfigPaths collection) {
        collectionName = config.get(collection.getName());
    }

    protected T getDocument(String id, String fieldNameId, Class<T> classType) {
        Gson gson = getGson();
        Document doc = getDocument(id, fieldNameId);
        if(doc == null) return null;
        return gson.fromJson(doc.toJson(), classType);
    }

    protected Document getDocument(String id, String fieldNameId) {
        MongoCollection<Document> collection = getCollection();
        return collection.find(Filters.eq(fieldNameId, id)).first();
    }

    protected abstract Document createDocument(T model);

    public boolean insertDocuments(Document... document) {
        if(Connector.client == null) return false;

        try {
            MongoCollection<Document> collection = getCollection();
            if (document.length == 1) collection.insertOne(document[0]);
            else collection.insertMany(List.of(document));
            log.info("Inserted data to db");
            return true;
        } catch (Exception e) {
            log.info("Could not insert data to database: {}", e.getMessage());
            return false;
        }
    }

    protected List<T> getDocuments(Class<T> className) {
        MongoCollection<Document> collection = getCollection();
        List<T> list = new ArrayList<>();
        Gson gson = getGson();

        try {
            for (Document document : collection.find()) {
                T model = gson.fromJson(document.toJson(), className);
                list.add(model);
            }
        } catch (Exception e) {
            log.info("Could not get data from database - " + e.getMessage());
        }

        return list;
    }

    public boolean deleteDocument(Document... documents) {
        if(Connector.client == null) return false;

        try {
            MongoCollection<Document> collection = getCollection();
            for (Document doc : documents) {
                Bson query = eq(id, doc.get(id));
                collection.deleteOne(query);
            }

            log.info("Removed data from db");
            return true;
        } catch (Exception e) {
            log.info("Could not delete data from db: " + e.getMessage());
            return false;
        }
    }

    public boolean replaceDocument(Document document) {
        if(Connector.client == null) return false;

        try {
            MongoCollection<Document> collection = getCollection();
            Bson query = eq(id, document.get(id));
            collection.replaceOne(query, document);
            log.info("Updated data in db");
            return true;
        } catch (Exception e) {
            log.info("Could not update data in db: " + e.getMessage());
            return false;
        }
    }

    protected MongoCollection<Document> getCollection() {
        var providers = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder()
                        .automatic(true)
                        .build()));
        return mongoDatabase.getCollection(collectionName).withCodecRegistry(providers);
    }

    protected Gson getGson() {
        JsonDeserializer<ObjectId> dec = (jsonElement, type, jsonDeserializationContext) ->
                new ObjectId(jsonElement.getAsJsonObject().get("$oid").getAsString());
        return new GsonBuilder().registerTypeAdapter(ObjectId.class, dec).create();
    }
}
