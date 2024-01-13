package events.abstracts;

import discord.messages.DeleteMessages;
import discord.messages.SendMessages;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import events.interfaces.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class EventsMethods implements EventListener {
    protected final static Logger logINFO = LoggerFactory.getLogger(EventsMethods.class);
    protected final SendMessages sendMessages;
    protected final DeleteMessages deleteMessages;

    protected EventsMethods() {
        sendMessages = new SendMessages();
        deleteMessages = new DeleteMessages();
        new Thread(this::activateEvent).start();
    }

    protected Snowflake getChannelId(ChatInputInteractionEvent event) {
        Optional<ApplicationCommandInteractionOptionValue> value = event.getOptions().get(0).getValue();
        return value.map(ApplicationCommandInteractionOptionValue::asSnowflake).orElse(null);
    }

    protected abstract void activateEvent();

    protected void saveSetChannel(ChatInputInteractionEvent event) {
        //TODO SQL
    }
}
