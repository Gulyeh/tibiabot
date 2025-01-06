package events.abstracts;

import com.google.common.collect.Iterables;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.discordjson.json.ComponentData;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Iterator;
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

    protected Iterable<LayoutComponent> splitActionRows(List<Button> buttons) {
        Iterable<List<Button>> subSets = Iterables.partition(buttons, 5);
        Iterator<List<Button>> partition = subSets.iterator();
        List<LayoutComponent> rows = new ArrayList<>();
        while(partition.hasNext()) {
            rows.add(ActionRow.of(partition.next()));
        }
        return rows;
    }

    protected String getId(ButtonInteractionEvent event) {
        return event.getCustomId();
    }

    protected Message getMessage(ButtonInteractionEvent event) {
        return event.getInteraction().getMessage().get();
    }
}
