package events.abstracts;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import events.interfaces.Listener;
import mongo.models.GuildModel;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mongo.DocumentActions.*;

public abstract class DiscordEvent implements Listener {

    protected final static Logger logINFO = LoggerFactory.getLogger(DiscordEvent.class);

    protected void removeGuild(Snowflake guildId) {
        try {
            if(!CacheData.isGuildCached(guildId)) throw new Exception("Guild does not exist in cache");

            Document doc = getDocument(guildId);
            if(doc == null) throw new Exception("Could not find guild in db");
            if(!deleteDocument(doc)) throw new Exception("Could not delete document");

            CacheData.removeGuild(guildId);
            logINFO.info("Successfully removed guild");
        } catch (Exception e) {
            logINFO.info("Could not remove guild: " + e.getMessage());
        }
    }

    protected void removeChannel(Snowflake guildId, Snowflake channelId) {
        try {
            if(!CacheData.getChannelsCache().containsKey(guildId)) throw new Exception("Guild does not exist in cache");

            GuildModel doc = getDocument(guildId, GuildModel.class);
            if(doc == null) throw new Exception("Could not find guild in db");
            if(!doc.getChannels().isChannelUsed(channelId)) return;

            doc.getChannels().removeChannel(channelId.asString());
            if(!replaceDocument(createDocument(doc))) throw new Exception("Could not remove channelId from db");

            CacheData.removeChannel(guildId, channelId);
            logINFO.info("Successfully removed channel");
        } catch (Exception e) {
            logINFO.info("Could not remove channel: " + e.getMessage());
        }
    }


}
