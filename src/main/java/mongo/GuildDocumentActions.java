package mongo;

import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import mongo.abstracts.DocumentActions;
import mongo.models.GuildModel;
import org.bson.Document;
import utils.Configurator;

@Slf4j
public final class GuildDocumentActions extends DocumentActions {
    public GuildDocumentActions() {
        super(Configurator.ConfigPaths.DB_COLLECTION_GUILDS);
    }

    public static Document getDocument(Snowflake guildId) {
        return getDocument(guildId.asString(), "guildId");
    }

    public static <T> T getDocument(Snowflake guildId, Class<T> classType) {
        return getDocument(guildId.asString(), "guildId", classType);
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
}
