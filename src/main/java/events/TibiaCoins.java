package events;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import events.abstracts.EmbeddableEvent;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.tibiaCoins.TibiaCoinsService;
import services.tibiaCoins.models.PriceModel;
import services.tibiaCoins.models.Prices;

import java.util.ArrayList;
import java.util.List;

import static builders.commands.names.CommandsNames.tibiaCoinsCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendEmbeddedMessages;

public class TibiaCoins extends EmbeddableEvent implements Channelable {
    private final TibiaCoinsService tibiaCoinsService;

    public TibiaCoins() {
        tibiaCoinsService = new TibiaCoinsService();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(tibiaCoinsCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                return setDefaultChannel(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @Override
    public String getEventName() {
        return EventName.getTibiaCoins();
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    protected void activateEvent() {
        logINFO.info("Activating " + getEventName());
        while(true) {
            try {
                logINFO.info("Executing thread " + getEventName());
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(3600000);
                }
            }
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake id = getChannelId((ChatInputInteractionEvent) event);
        if(id == null) return event.createFollowup("Could not find channel");

        GuildMessageChannel channel = client.getChannelById(id).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + id.asString() + ">");

        processData(channel, tibiaCoinsService.getPrices());
        return event.createFollowup("Set default Tibia Coins channel to <#" + id.asString() + ">");
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        for(Prices data : ((PriceModel)model).getPrices()) {
            fields.add(buildEmbedField(data));
        }

        return fields;
    }

    @Override
    protected <T> void processData(GuildMessageChannel channel, T model) {
        deleteMessages(channel);

        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        sendEmbeddedMessages(channel,
                createEmbedFields(model),
                "Tibia Coins Prices",
                "(World name)\n(Buy price / Sell price)\n(checked at)",
                "",
                "",
                Color.RED);
    }

    private EmbedCreateFields.Field buildEmbedField(Prices data) {
        return EmbedCreateFields.Field.of(data.getWorld_name(),
                data.getBuy_average_price() + " / " + data.getSell_average_price() +"\n`(" + data.getCreated_at() + ")`",
                true );
    }
}
