package events.abstracts;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.discordjson.json.ComponentData;
import lombok.Getter;

import java.util.List;

public abstract class InteractionEvent extends EmbeddableEvent {
    @Getter
    protected final String buttonId;

    protected InteractionEvent(String buttonId) {
        this.buttonId = buttonId;
    }

    protected List<ComponentData> getInteractionButtons(Message message) {
        return message.getData()
                .components()
                .get().get(0)
                .components().get();
    }

    protected String getId(ButtonInteractionEvent event) {
        return event.getCustomId();
    }

    protected Message getMessage(ButtonInteractionEvent event) {
        return event.getInteraction().getMessage().get();
    }
}
