package events.abstracts;

import cache.DatabaseCacheData;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import events.interfaces.Listener;
import mongo.models.ChannelModel;
import mongo.models.GuildModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static cache.DatabaseCacheData.isGuildCached;
import static discord.Connector.client;
import static mongo.DocumentActions.*;

public abstract class EventsMethods implements Listener {

    protected final static Logger logINFO = LoggerFactory.getLogger(EventsMethods.class);

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

    protected Snowflake getUserId(ChatInputInteractionEvent event) {
        return event.getInteraction().getUser().getId();
    }

    protected boolean isUserAdministrator(ChatInputInteractionEvent event) {
       Member member = client.getMemberById(getGuildId(event), getUserId(event)).block();
       if(member == null) return false;
       PermissionSet permissions = member.getBasePermissions().block();
       if(permissions == null) return false;
       return permissions.asEnumSet().stream().anyMatch(x -> x.equals(Permission.ADMINISTRATOR));
    }

    protected boolean saveSetChannel(ChatInputInteractionEvent event) {
        try {
            Snowflake guildId = getGuildId(event);
            Snowflake channelId = getChannelId(event);
            EventTypes eventType = EventTypes.getEnum(getEventName());

            if (!isGuildCached(guildId)) {
                GuildModel model = new GuildModel();
                model.setChannels(new ChannelModel());
                model.setGuildId(guildId.asString());
                model.getChannels().setByEventType(eventType, channelId.asString());
                if(!insertDocuments(createDocument(model))) throw new Exception("Could not save model to database");
            } else {
                GuildModel model = getGuild(guildId);
                if (model.getChannels().isChannelUsed(channelId)) throw new Exception("This channel is already in use");
                model.getChannels().setByEventType(eventType, channelId.asString());
                if(!replaceDocument(createDocument(model))) throw new Exception("Could not update model in database");
            }

            DatabaseCacheData.addToChannelsCache(guildId, channelId, eventType);
            logINFO.info("Saved channel");
            return true;
        } catch (Exception e) {
            logINFO.info("Could not save channel: " + e.getMessage());
            return false;
        }
    }

    protected GuildModel getGuild(Snowflake guildId) throws Exception {
        GuildModel model = getDocument(guildId, GuildModel.class);
        if (model == null || model.get_id() == null) throw new Exception("Document is null");
        return model;
    }
}
