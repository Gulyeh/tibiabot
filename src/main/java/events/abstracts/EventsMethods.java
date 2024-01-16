package events.abstracts;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import events.interfaces.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
//        Snowflake guildId = getGuildId(event);
//        Snowflake channelId = getChannelId(event);
//        EventTypes eventType = EventTypes.getEnum(getEventName());
//        boolean guildExists = CacheData.getChannelsCache().containsKey(guildId);
//        HashMap<EventTypes, Snowflake> mapChannels;
        //TODO SQL
    }
}
