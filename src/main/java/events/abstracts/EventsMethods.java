package events.abstracts;

import cache.CacheData;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import events.interfaces.EventListener;
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

    protected void saveSetChannel(ChatInputInteractionEvent event) {
        try {
            Snowflake guildId = getGuildId(event);
            Snowflake channelId = getChannelId(event);
            EventTypes eventType = EventTypes.getEnum(getEventName());

            boolean guildExists = CacheData.getChannelsCache().containsKey(guildId);
            CacheData.addToChannelsCache(guildId, channelId, eventType);

            if (!guildExists) {
                GuildModel model = new GuildModel();
                model.setGuildId(guildId.toString());
                model.getChannels().setByEventType(eventType, channelId.asString());
                insertDocuments(createDocument(model));
            } else {
                GuildModel model = getDocument(guildId, GuildModel.class);
                if (model == null) return;
                model.getChannels().setByEventType(eventType, channelId.asString());
                replaceDocument(createDocument(model));
            }
            logINFO.info("Saved channel");
        } catch (Exception e) {
            logINFO.info("Could not save channel: " + e.getMessage());
        }
    }

    protected void saveSetWorld(String serverName, Snowflake guildId) {
        try {
            boolean guildExists = CacheData.getChannelsCache().containsKey(guildId);
            CacheData.addToWorldsCache(guildId, serverName);

            if (!guildExists) {
                GuildModel model = new GuildModel();
                model.setWorld(serverName);
                insertDocuments(createDocument(model));
            } else {
                GuildModel model = getDocument(guildId, GuildModel.class);
                if (model == null) return;
                model.setWorld(serverName);
                replaceDocument(createDocument(model));
            }

            logINFO.info("Saved server world");
        } catch (Exception e) {
            logINFO.info("Could not save world: " + e.getMessage());
        }
    }

    private Document createDocument(GuildModel model) {
        return new Document()
                .append("guildId", model.getGuildId())
                .append("world", model.getWorld())
                .append("channels", model.getChannels());
    }
}
