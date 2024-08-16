package events;

import cache.DatabaseCacheData;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import events.abstracts.EmbeddableEvent;
import events.interfaces.Channelable;
import events.utils.EventName;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;
import services.houses.HousesService;
import services.houses.models.HouseData;
import services.houses.models.HousesModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static builders.commands.names.CommandsNames.houseCommand;
import static discord.Connector.client;
import static discord.messages.DeleteMessages.deleteMessages;
import static discord.messages.SendMessages.sendEmbeddedMessages;

public class Houses extends EmbeddableEvent implements Channelable {

    private final HousesService housesService;

    public Houses(HousesService housesService) {
        this.housesService = housesService;
    }


    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(houseCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

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
        while (true) {
            try {
                logINFO.info("Executing thread " + getEventName());
                housesService.clearCache();
                executeEventProcess();
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
    protected <T> void processData(GuildMessageChannel channel, T model) {
        deleteMessages(channel);

        if (model == null) {
            logINFO.warn("model is null");
            return;
        }

        List<HousesModel> list = ((List<HousesModel>) model)
                .stream()
                .filter(x -> x.getHouse_list() != null &&
                        !x.getHouse_list().isEmpty())
                .toList();

        for (HousesModel house : list) {
            sendEmbeddedMessages(channel,
                    createEmbedFields(house),
                    house.getTown(),
                    "",
                    "",
                    "",
                    getRandomColor());
        }
    }

    @Override
    protected void executeEventProcess() {
        Set<Snowflake> guildIds = DatabaseCacheData.getChannelsCache().keySet();
        if(guildIds.isEmpty()) return;

        for (Snowflake guildId : guildIds) {
            Snowflake channel = DatabaseCacheData.getChannelsCache()
                    .get(guildId)
                    .get(EventTypes.HOUSES);
            if(channel == null || channel.asString().isEmpty()) continue;

            Guild guild = client.getGuildById(guildId).block();
            if(guild == null) continue;

            GuildMessageChannel guildChannel = (GuildMessageChannel)guild.getChannelById(channel).block();
            if(guildChannel == null) continue;

            processData(guildChannel, housesService.getHouses(guildId));
        }
    }

    @Override
    public <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        Snowflake guildId = getGuildId((ChatInputInteractionEvent) event);

        if (channelId == null || guildId == null) return event.createFollowup("Could not find channel or guild");
        if (!DatabaseCacheData.getWorldCache().containsKey(guildId))
            return event.createFollowup("You have to set tracking world first");

        GuildMessageChannel channel = client.getChannelById(channelId).ofType(GuildMessageChannel.class).block();
        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");
        processData(channel, housesService.getHouses(guildId));
        return event.createFollowup("Set default Houses event channel to <#" + channelId.asString() + ">");
    }

    @Override
    protected <T> List<EmbedCreateFields.Field> createEmbedFields(T model) {
        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        for (HouseData data : ((HousesModel) model).getHouse_list()) {
            fields.add(buildEmbedField(data, ((HousesModel) model).getWorld()));
        }

        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(HouseData data, String world) {
        String auctionLink = "[Auction](https://www.tibia.com/community/?subtopic=houses&page=view&houseid="
        + data.getHouse_id() + "&world=" + world + ")";

        return EmbedCreateFields.Field.of(data.getName() + " (" + data.getHouse_id() + ")",
                "SQM: " + data.getSize() + "\nRent: " + data.getRent() + " gold\n\n``Current bid: " +
                        data.getAuction().getCurrent_bid() + " gold\nTime left: " + data.getAuction().getTime_left() +
                        "``\n\n" + auctionLink,
                true);
    }
}
