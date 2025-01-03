package events.lootSplitter;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ComponentData;
import events.abstracts.EventsMethods;
import lombok.Getter;
import reactor.core.publisher.Mono;
import services.lootSplitter.model.SplitLootModel;
import services.lootSplitter.model.SplittingMember;
import services.lootSplitter.model.TransferData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static discord.Connector.client;

@Getter
public class SplitterInteractionsHandler extends EventsMethods {
    private final String splittingButtonId = "transfer";

    @Override
    public void executeEvent() {
        client.on(ButtonInteractionEvent.class, event -> {
            if (!event.getCustomId().contains(splittingButtonId)) return Mono.empty();
            String id = event.getCustomId();
            String splitterName = id.split("_")[1];
            Message message = event.getInteraction().getMessage().get();
            List<ComponentData> interactionButtons = message.getData()
                    .components()
                    .get().get(0)
                    .components().get();

            List<Button> buttons = updateButtons(interactionButtons, id, splitterName);

            StringBuilder builder = new StringBuilder();
            splitInteractionIdToTransfers(id).forEach((k, v) -> builder
                    .append("Very well. ")
                    .append(splitterName)
                    .append(" has transferred ")
                    .append(v)
                    .append(" gold to ")
                    .append(k)
                    .append("\n"));

            message.edit().withComponents(ActionRow.of(buttons)).subscribe();
            return event.reply(builder.toString());
        }).subscribe();
    }

    public List<Button> getSplittingButtons(SplitLootModel model) {
        List<Button> buttons = new ArrayList<>();
        for(SplittingMember member : model.getMembers()) {
            if(member.getTransfers().isEmpty()) continue;
            buttons.add(Button.primary(createButtonId(member),
                    member.getName() + " has " + member.getTransfers().size() + " transfer(s) pending"));
        }
        return buttons;
    }

    private String createButtonId(SplittingMember member) {
        StringBuilder buttonId = new StringBuilder(member.getName());
        for(TransferData data : member.getTransfers()) {
            buttonId.append("_[")
                    .append(data.getTransferTo())
                    .append("_")
                    .append(data.getTransferAmount())
                    .append("]");
        }
        return getSplittingButtonId() + "_" + buttonId;
    }


    private List<Button> updateButtons(List<ComponentData> interactionButtons, String customId, String splitterName) {
        List<Button> buttons = new ArrayList<>();
        for (ComponentData data : interactionButtons) {
            if(data.customId().get().equals(customId)) {
                buttons.add(Button.primary(data.customId().get(), splitterName + " sent transfers").disabled());
                continue;
            }

            Button btn = Button.primary(data.customId().get(), data.label().get());
            if(!data.disabled().isAbsent() && data.disabled().get())
                btn = btn.disabled();
            buttons.add(btn);
        }

        return buttons;
    }

    private Map<String, String> splitInteractionIdToTransfers(String customId) {
        Map<String, String> map = new HashMap<>();

        Pattern pattern = Pattern.compile("\\[(\\w+_\\d+)]");
        Matcher matcher = pattern.matcher(customId);

        while (matcher.find()) {
            String[] transfer = matcher.group(1).split("_");
            map.put(transfer[0], transfer[1]);
        }

        return map;
    }


    @Override
    public String getEventName() {
        return "Splitter Interactions Handler";
    }
}
