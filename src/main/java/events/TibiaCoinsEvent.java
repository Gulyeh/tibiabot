package events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import events.interfaces.Channelable;
import events.interfaces.EventListener;
import reactor.core.publisher.Mono;
import services.tibiaCoins.TibiaCoinsService;
import services.tibiaCoins.models.PriceModel;
import services.tibiaCoins.models.Prices;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static builders.Commands.names.CommandsNames.tibiaCoinsCommand;
import static discord.Connector.client;

public class TibiaCoinsEvent extends EventsMethods implements EventListener, Channelable {
    private final TibiaCoinsService tibiaCoinsService;

    public TibiaCoinsEvent() {
        tibiaCoinsService = new TibiaCoinsService();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            if(!event.getCommandName().equals(tibiaCoinsCommand)) return null;
            return event.deferReply().withEphemeral(true).then(setDefaultChannel(event));
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @Override
    public String getEventName() {
        return "Tibia Coins Event";
    }

    @Override
    protected void activateEvent() {
        logINFO.info("Activating " + getEventName());
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake id = getChannelId((ChatInputInteractionEvent) event);
        if(id == null) return event.createFollowup("Could not find channel");

        Channel channel = event.getClient().getChannelById(id).block();
        saveSetChannel((ChatInputInteractionEvent) event);

        deleteMessages.deleteMessages(channel);

        sendMessages.sendEmbeddedMessages(channel,
                createEmbedFields(),
                "Tibia Coins Prices",
                "(world name)\n(Avg. buy value / Avg. sell value)\n(checked at)",
                "",
                "",
                Color.RED);
        
        return event.createFollowup("Set default Tibia coins channel to <#" + id.asString() + ">");
    }

    @Override
    protected List<EmbedCreateFields.Field> createEmbedFields() {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        PriceModel model = tibiaCoinsService.getPrices();

        if(model == null) {
            logINFO.info("Could not create embed from empty model");
            return null;
        }

        for(Prices data : model.getPrices()) {
            fields.add(buildEmbedField(data));
        }

        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(Prices data) {
        return EmbedCreateFields.Field.of(data.getWorld_name(), data.getBuy_average_price() + " / " + data.getSell_average_price() +"\n`(" + data.getCreated_at() + ")`", true );
    }
}
