package events;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import events.abstracts.EventsMethods;
import events.utils.EventName;
import mongo.models.GuildModel;
import reactor.core.publisher.Mono;

import static builders.commands.names.CommandsNames.setMinimumDeathsLevelCommand;
import static cache.DatabaseCacheData.addMinimumDeathLevelCache;
import static discord.Connector.client;
import static mongo.DocumentActions.createDocument;
import static mongo.DocumentActions.replaceDocument;

public class MinimumDeathLevel extends EventsMethods {

    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(setMinimumDeathsLevelCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                if (!isUserAdministrator(event)) return event.createFollowup("You do not have permissions to use this command");

                return setLevel(event);
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return EventName.getMinimumDeathLevel();
    }

    private Mono<Message> setLevel(ChatInputInteractionEvent event) throws Exception {
        GuildModel model = getGuild(getGuildId(event));
        if(model.getChannels().getDeathTracker().isEmpty()) return event.createFollowup("Death tracker channel has to be set first.");

        int level = Integer.parseInt(getTextParameter(event));
        if(level < 8) return event.createFollowup("Level cannot be lower than 8");

        model.setDeathMinimumLevel(level);
        if(!replaceDocument(createDocument(model))) throw new Exception("Could not update model in database");

        addMinimumDeathLevelCache(getGuildId(event), level);
        return event.createFollowup("Set minimum death level to " + level);
    }
}
