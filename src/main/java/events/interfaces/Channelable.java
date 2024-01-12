package events.interfaces;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface Channelable {
   <T extends ApplicationCommandInteractionEvent> Mono<Message> setDefaultChannel(T event);
}
