package events.registration;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import events.abstracts.EventsMethods;
import events.utils.EventName;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import services.registration.UnregisterCharacter;

import static builders.commands.names.CommandsNames.unregisterCommand;
import static discord.Connector.client;

@Slf4j
public class CharacterUnregistration extends EventsMethods {

    private final UnregisterCharacter unregisterCharacter;

    public CharacterUnregistration() {
        unregisterCharacter = new UnregisterCharacter();
    }


    @Override
    public void executeEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(unregisterCommand)) return Mono.empty();
                event.deferReply().withEphemeral(true).subscribe();
                boolean unregistered = unregisterCharacter.unregisterCharacter(getTextParameter(event), getUserId(event));
                return event.createFollowup(unregistered ? "Unregistered character successfully" : "Character could not be unregistered");
            } catch (Exception e) {
                log.error(e.getMessage());
                return event.createFollowup("Could not execute command");
            }
        }).subscribe();
    }

    @Override
    public String getEventName() {
        return EventName.unregisterCharacter;
    }
}
