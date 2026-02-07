package events.abstracts;

import cache.guilds.GuildCacheData;
import discord4j.common.util.Snowflake;
import events.interfaces.Listener;
import lombok.extern.slf4j.Slf4j;
import mongo.GuildDocumentActions;
import mongo.models.GuildModel;
import org.bson.Document;

@Slf4j
public abstract class DiscordEvent implements Listener {
    protected final GuildDocumentActions guildDocumentActions;

    public DiscordEvent() {
        guildDocumentActions = GuildDocumentActions.getInstance();
    }

    protected void removeGuild(Snowflake guildId) {
        try {
            if(!GuildCacheData.isGuildCached(guildId)) throw new Exception("Guild does not exist in cache");

            Document doc = guildDocumentActions.getDocument(guildId);
            if(doc == null) throw new Exception("Could not find guild in db");
            if(!guildDocumentActions.deleteDocument(doc)) throw new Exception("Could not delete document");

            GuildCacheData.removeGuild(guildId);
            log.info("Successfully removed guild");
        } catch (Exception e) {
            log.info("Could not remove guild: {}", e.getMessage());
        }
    }

    protected void removeChannel(Snowflake guildId, Snowflake channelId) {
        try {
            if(!GuildCacheData.channelsCache.containsKey(guildId)) throw new Exception("Guild does not exist in cache");

            GuildModel doc = guildDocumentActions.getDocumentModel(guildId);
            if(doc == null) throw new Exception("Could not find guild in db");
            if(!doc.getChannels().isChannelUsed(channelId)) return;

            doc.getChannels().removeChannel(channelId.asString());
            if(!guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(doc)))
                throw new Exception("Could not remove channelId from db");

            GuildCacheData.removeChannel(guildId, channelId);
            log.info("Successfully removed channel");
        } catch (Exception e) {
            log.info("Could not remove channel: {}", e.getMessage());
        }
    }
}
