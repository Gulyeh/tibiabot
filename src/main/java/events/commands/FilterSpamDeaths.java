package events.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventsMethods;
import lombok.extern.slf4j.Slf4j;
import mongo.models.GuildModel;
import reactor.core.publisher.Mono;

import static builders.commands.names.CommandsNames.setDeathSpamFilterCommand;
import static cache.guilds.GuildCacheData.*;
import static discord.Connector.client;
import static events.utils.EventName.filterSpamDeaths;

@Slf4j
public class FilterSpamDeaths extends EventsMethods {

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setDeathSpamFilterCommand.getCommandName())) return Mono.empty();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");
                event.deferReply().withEphemeral(true).subscribe();
                return setFilterValue(event);
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    private Mono<Message> setFilterValue(ChatInputInteractionEvent event) throws Exception {
        Boolean filterDeaths = getBooleanParameter(event);
        Snowflake guildId = getGuildId(event);
        GuildModel guildModel = getGuild(guildId);
        if(guildModel.getChannels().getDeathTracker().isEmpty()) return event.createFollowup("Death tracker channel has to be set first.");

        if(filterDeaths && antiSpamDeathCache.contains(guildId)) return event.createFollowup("Deaths filter is already active");
        else if(!filterDeaths && !antiSpamDeathCache.contains(guildId)) return event.createFollowup("Deaths filter is already deactivated");

        guildModel.setFilterDeaths(filterDeaths);
        if(!guildDocumentActions.replaceDocument(guildDocumentActions.createDocument(guildModel)))
            throw new Exception("Could not update model in database");

        if(filterDeaths) addToAntiSpamDeath(guildId);
        else removeAntiSpamDeath(guildId);

        return event.createFollowup("Deaths will be " + (filterDeaths ? "filtered from now on!" : "no longer filtered!"));
    }

    @Override
    public String getEventName() {
        return filterSpamDeaths;
    }
}
