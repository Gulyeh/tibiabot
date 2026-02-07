package handlers;

import com.google.common.collect.Iterables;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ComponentData;
import lombok.Getter;
import observers.InteractionObserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
public class InteractionHandler {
    private final String buttonId;
    private final InteractionObserver observer;

    public InteractionHandler(String buttonId, InteractionObserver observer) {
        this.buttonId = buttonId;
        this.observer = observer;
    }

    public InteractionHandler(String buttonId) {
        this.buttonId = buttonId;
        this.observer = new InteractionObserver();
    }

    public InteractionHandler() {
        this("");
    }

    public List<ComponentData> getInteractionButtons(Message message) {
        return message.getData()
                .components()
                .get().get(0)
                .components().get();
    }

    public Iterable<LayoutComponent> splitActionRows(List<Button> buttons) {
        Iterable<List<Button>> subSets = Iterables.partition(buttons, 5);
        Iterator<List<Button>> partition = subSets.iterator();
        List<LayoutComponent> rows = new ArrayList<>();
        while(partition.hasNext()) {
            rows.add(ActionRow.of(partition.next()));
        }
        return rows;
    }

    public List<Button> toggleLockButton(List<ComponentData> buttons, boolean lock) {
        List<Button> buttonsList = new ArrayList<>();
        for(ComponentData component : buttons) {
            if(!component.customId().get().contains(getButtonId())) {
                buttonsList.add((Button) Button.fromData(component));
                continue;
            }

            Button btn = (Button) Button.fromData(component);
            btn = lock ? btn.disabled() : btn.disabled(false);
            buttonsList.add(btn);
        }
        return buttonsList;
    }

    public String getId(ButtonInteractionEvent event) {
        return event.getCustomId();
    }

    public Message getMessage(ButtonInteractionEvent event) {
        return event.getInteraction().getMessage().get();
    }
}
