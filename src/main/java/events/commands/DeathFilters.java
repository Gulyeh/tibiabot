package events.commands;

import apis.tibiaData.TibiaDataAPI;
import apis.tibiaData.model.deathtracker.CharacterResponse;
import builders.commands.names.CommandOptionNames;
import cache.enums.EventTypes;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import events.abstracts.EventMethods;
import events.utils.EventName;
import lombok.extern.slf4j.Slf4j;
import mongo.models.GuildModel;
import reactor.core.publisher.Mono;
import services.deathTracker.enums.DeathFilterAction;
import services.deathTracker.enums.DeathFilterType;

import java.util.Optional;

import static builders.commands.names.CommandsNames.*;
import static cache.guilds.GuildCacheData.*;
import static discord.Connector.client;

@Slf4j
public final class DeathFilters extends EventMethods {

    private final TibiaDataAPI api;

    public DeathFilters() {
        api = new TibiaDataAPI();
    }

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(filterDeathsCommand.getCommandName())) return Mono.empty();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");
                event.deferReply().withEphemeral(true).subscribe();
                return changeFilter(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();

        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setFilteredDeathsCommand.getCommandName())) return Mono.empty();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");
                event.deferReply().withEphemeral(true).subscribe();
                return setDefaultChannel(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return EventName.filterDeathTracker;
    }

    private Mono<Message> changeFilter(ChatInputInteractionEvent event) throws Exception {
        Snowflake guildId = getGuildId(event);
        GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.FILTERED_DEATH_TRACKER);

        if (guildChannel == null)
            return event.createFollowup("You have to set filtered deaths channel first to be able to manage filters");

        DeathFilterType type = parseEnum(event, CommandOptionNames.FILTER_TYPE, DeathFilterType.class);
        DeathFilterAction action = parseEnum(event, CommandOptionNames.FILTER_ACTION, DeathFilterAction.class);
        String value = getTextParameter(event, filterDeathsCommand.getOptions()
                .stream().filter(x -> x.getOptionName().equals(CommandOptionNames.FILTER_VALUE))
                .findFirst().get()).toLowerCase();

        GuildModel guildModel = getGuild(guildId);

        return switch (action) {
            case ADD -> handleAddFilter(event, guildId, guildModel, type, value);
            case REMOVE -> handleRemoveFilter(event, guildId, guildModel, type, value);
        };
    }

    private <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event) {
        Snowflake guildId = getGuildId(event);
        GuildMessageChannel guildChannel = getGuildChannel(guildId, EventTypes.DEATH_TRACKER);
        if(guildChannel == null) return event.createFollowup("You have to set deaths channel first to be able to use death filter");

        Snowflake channelId = getChannelId((ChatInputInteractionEvent) event);
        if (channelId == null) return event.createFollowup("Could not find channel");

        if(!saveSetChannel((ChatInputInteractionEvent) event))
            return event.createFollowup("Could not set channel <#" + channelId.asString() + ">");

        addToChannelsCache(guildId, channelId, EventTypes.FILTERED_DEATH_TRACKER);
        return event.createFollowup("Set default Filtered Death Tracker channel to <#" + channelId.asString() + ">");
    }

    private <T extends Enum<T>> T parseEnum(ChatInputInteractionEvent event, String option, Class<T> enumClass) {
        String value = getTextParameter(event, filterDeathsCommand.getOptions()
                .stream().filter(x -> x.getOptionName().equals(option))
                .findFirst().get());
        return Enum.valueOf(enumClass, value.toUpperCase());
    }

    private Mono<Message> handleAddFilter(ChatInputInteractionEvent event, Snowflake guildId, GuildModel guildModel,
                                          DeathFilterType type, String value) throws Exception {
        if (type == DeathFilterType.GUILD) {
            if (guildModel.getDeathFilter().getGuilds().stream().anyMatch(x -> x.equalsIgnoreCase(value)))
                return event.createFollowup("Guild **" + value + "** has been already added to filters");

            apis.tibiaData.model.guilds.GuildModel guild = api.getGuild(value);
            boolean exists = guild.getGuild() != null && guild.getGuild().getName().equalsIgnoreCase(value)
                    && guild.getGuild().getWorld().equalsIgnoreCase(guildModel.getWorld());

            if (!exists)
                return event.createFollowup("Guild **" + value + "** does not exist on world " + guildModel.getWorld());

            String guildName = guild.getGuild().getName();
            guildModel.getDeathFilter().getGuilds().add(guildName);
            updateGuildModel(guildModel);
            addDeathFilterGuildCache(guildId, guildName);

            return event.createFollowup("Successfully added guild **" + guildName + "** to filters");

        } else {
            if (guildModel.getDeathFilter().getNames().stream().anyMatch(x -> x.equalsIgnoreCase(value)))
                return event.createFollowup("Character **" + value + "** has been already added to filters");

            CharacterResponse charData = api.getCharacterData(value);
            boolean exists = charData.getCharacter() != null
                    && charData.getCharacter().getCharacter().getName().equalsIgnoreCase(value)
                    && charData.getCharacter().getCharacter().getWorld().equalsIgnoreCase(guildModel.getWorld());

            if (!exists)
                return event.createFollowup("Character **" + value + "** does not exist on world " + guildModel.getWorld());

            String characterName = charData.getCharacter().getCharacter().getName();
            guildModel.getDeathFilter().getNames().add(characterName);
            updateGuildModel(guildModel);
            addDeathFilterNameCache(guildId, characterName);

            return event.createFollowup("Successfully added character **" + characterName + "** to filters");
        }
    }

    private Mono<Message> handleRemoveFilter(ChatInputInteractionEvent event, Snowflake guildId, GuildModel guildModel,
                                             DeathFilterType type, String value) throws Exception {
        if (type == DeathFilterType.GUILD) {
            Optional<String> guildName = guildModel.getDeathFilter().getGuilds().stream()
                    .filter(x -> x.equalsIgnoreCase(value)).findFirst();

            if (guildName.isEmpty())
                return event.createFollowup("Guild **" + value + "** does not exist in filters");

            guildModel.getDeathFilter().getGuilds().remove(guildName.get());
            updateGuildModel(guildModel);
            removeDeathFilterGuildCache(guildId, guildName.get());

            return event.createFollowup("Successfully removed guild **" + guildName.get() + "** from filters");

        } else {
            Optional<String> characterName = guildModel.getDeathFilter().getNames().stream()
                    .filter(x -> x.equalsIgnoreCase(value)).findFirst();

            if (characterName.isEmpty())
                return event.createFollowup("Character **" + value + "** does not exist in filters");

            guildModel.getDeathFilter().getNames().remove(characterName.get());
            updateGuildModel(guildModel);
            removeDeathFilterNameCache(guildId, characterName.get());

            return event.createFollowup("Successfully removed character **" + characterName.get() + "** from filters");
        }
    }

    private void updateGuildModel(GuildModel guildModel) throws Exception {
        if (!guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(guildModel)))
            throw new Exception("Could not update model in database");
    }
}
