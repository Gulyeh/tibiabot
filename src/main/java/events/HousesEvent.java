package events;

import cache.CacheData;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import events.abstracts.EmbeddableEvent;
import events.abstracts.EventsMethods;
import events.interfaces.Channelable;
import events.interfaces.Worldable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.houses.HousesService;
import services.houses.models.HouseData;
import services.houses.models.HousesModel;
import services.worlds.models.WorldData;
import services.worlds.models.WorldModel;

import java.util.ArrayList;
import java.util.List;

import static builders.Commands.names.CommandsNames.houseCommand;
import static builders.Commands.names.CommandsNames.worldCommand;
import static discord.Connector.client;

public class HousesEvent extends EmbeddableEvent implements Channelable {

    private final HousesService housesService;

    public HousesEvent() {
        housesService = new HousesService();
    }


    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(houseCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                return setDefaultChannel(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).filter(message -> !message.getAuthor().map(User::isBot).orElse(true)).subscribe();
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("InfiniteLoopStatement")
    protected void activateEvent() {
        logINFO.info("Activating " + getEventName());
        while(true) {
            try {
                logINFO.info("Executing thread Houses");
            } catch (Exception e) {
                logINFO.info(e.getMessage());
            } finally {
                synchronized (this) {
                    wait(1800000);
                }
            }
        }
    }

    @Override
    public String getEventName() {
        return EventName.getHouses();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    protected <T> void sendMessage(GuildMessageChannel channel, T model) {
        deleteMessages.deleteMessages(channel);

        if(model == null) {
            logINFO.warn("model is null");
            return;
        }

        List<HousesModel> list = ((List<HousesModel>) model)
                .stream()
                .filter(x -> x.getHouse_list() != null &&
                        !x.getHouse_list().isEmpty())
                .toList();

        for(HousesModel house : list){
            sendMessages.sendEmbeddedMessages(channel,
                    createEmbedFields(house),
                    house.getTown(),
                    "",
                    "",
                    "",
                    Color.GRAY);
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);
        if(channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if(!CacheData.getWorldCache().containsKey(guildId)) return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        saveSetChannel((ChatInputInteractionEvent) event);
        sendMessage(channel, housesService.getHouses(guildId));
        return event.createFollowup("Set default Houses event channel to <#" + channelId.asString() + ">");
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        if(model == null) {
            logINFO.info("Could not create embed from empty model");
            return new ArrayList<>();
        }

        for (HouseData data : ((HousesModel) model).getHouse_list()) {
            fields.add(buildEmbedField(data));
        }

        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(HouseData data) {
        return EmbedCreateFields.Field.of(data.getName() + " (" + data.getHouse_id() + ")",
                "SQM: " + data.getSize() + "\nRent: " + data.getRent() + " gold\n\n``Current bid: " +
                        data.getAuction().getCurrent_bid() + " gold\nTime left: " + data.getAuction().getTime_left() + "``",
                true);
    }
}
