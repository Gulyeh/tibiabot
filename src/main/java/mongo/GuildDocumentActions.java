package mongo;

import discord4j.common.util.Snowflake;
import lombok.extern.slf4j.Slf4j;
import mongo.abstracts.DocumentActions;
import mongo.models.GuildModel;
import org.bson.Document;
import utils.Configurator;

import java.util.List;

@Slf4j
public class GuildDocumentActions extends DocumentActions<GuildModel> {

    public GuildDocumentActions() {
        super(Configurator.ConfigPaths.DB_COLLECTION_GUILDS);
    }

    public static GuildDocumentActions getInstance() {
        return getInstance(GuildDocumentActions.class);
    }

    public GuildModel getDocumentModel(Snowflake guildId) {
        return getDocument(guildId.asString(), "guildId", GuildModel.class);
    }

    public Document getDocument(Snowflake guildId) {
        return getDocument(guildId.asString(), "guildId");
    }

    public List<GuildModel> getDocuments() {
        return getDocuments(GuildModel.class);
    }

    public Document createDocument(GuildModel model) {
        Document doc = new Document()
                .append("guildId", model.getGuildId())
                .append("world", model.getWorld())
                .append("minimumDeathLevel", model.getDeathMinimumLevel())
                .append("filterDeaths", model.isFilterDeaths())
                .append("channels", model.getChannels())
                .append("deathFilter", model.getDeathFilter());

        if(model.get_id() != null) doc.append("_id", model.get_id());
        return doc;
    }
}
