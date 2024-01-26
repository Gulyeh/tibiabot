package events.abstracts;

import cache.CacheData;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import events.interfaces.EventListener;
import mongo.models.ChannelModel;
import mongo.models.GuildModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static mongo.DocumentActions.*;

public abstract class EventsMethods implements EventListener {

    protected final static Logger logINFO = LoggerFactory.getLogger(EventsMethods.class);

    protected EventsMethods() {
        new Thread(this::activateEvent).start();
    }

    protected Snowflake getChannelId(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(x -> Snowflake.of(x.getRaw().replaceAll("\\D", ""))).orElse(null);
    }

    protected Snowflake getGuildId(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuildId().get();
    }

    protected String getTextParameter(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(ApplicationCommandInteractionOptionValue::getRaw).orElse(null);
    }

    protected abstract void activateEvent();

    protected boolean saveSetChannel(ChatInputInteractionEvent event) {
        try {
            Snowflake guildId = getGuildId(event);
            Snowflake channelId = getChannelId(event);
            EventTypes eventType = EventTypes.getEnum(getEventName());

            GuildModel doc = getDocument(guildId, GuildModel.class);
            boolean guildExists = doc != null && doc.getGuildId() != null;


            if (!guildExists) {
                GuildModel model = new GuildModel();
                model.setChannels(new ChannelModel());
                model.setGuildId(guildId.asString());
                model.getChannels().setByEventType(eventType, channelId.asString());
                if(!insertDocuments(createDocument(model))) throw new Exception("Could not save model to database");
            } else {
                GuildModel model = getDocument(guildId, GuildModel.class);
                if (model == null || model.get_id() == null) throw new Exception("Document is null");
                else if (model.getChannels().isChannelUsed(channelId)) throw new Exception("This channel is already in use");
                model.getChannels().setByEventType(eventType, channelId.asString());
                if(!replaceDocument(createDocument(model))) throw new Exception("Could not update model in database");
            }

            CacheData.addToChannelsCache(guildId, channelId, eventType);
            logINFO.info("Saved channel");
            return true;
        } catch (Exception e) {
            logINFO.info("Could not save channel: " + e.getMessage());
            return false;
        }
    }

    protected boolean saveSetWorld(String serverName, Snowflake guildId) {
        try {

            GuildModel doc = getDocument(guildId, GuildModel.class);
            boolean guildExists = doc != null && doc.getGuildId() != null;

            if (!guildExists) {
                GuildModel model = new GuildModel();
                model.setChannels(new ChannelModel());
                model.setGuildId(guildId.asString());
                model.setWorld(serverName);
                if(!insertDocuments(createDocument(model))) throw new Exception("Could not save model to database");
            } else {
                GuildModel model = getDocument(guildId, GuildModel.class);
                if (model == null || model.get_id() == null) throw new Exception("Document is null");
                model.setWorld(serverName);
                if(!replaceDocument(createDocument(model))) throw new Exception("Could not update model in database");
            }

            CacheData.addToWorldsCache(guildId, serverName);
            logINFO.info("Saved server world");
            return true;
        } catch (Exception e) {
            logINFO.info("Could not save world: " + e.getMessage());
            return false;
        }
    }

    private Document createDocument(GuildModel model) {
        Document doc = new Document()
                .append("guildId", model.getGuildId())
                .append("world", model.getWorld())
                .append("channels", model.getChannels());

        if(model.get_id() != null) doc.append("_id", model.get_id());

        return doc;
    }
}
