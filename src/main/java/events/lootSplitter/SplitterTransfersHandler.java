package events.lootSplitter;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ComponentData;
import events.abstracts.InteractionEvent;
import reactor.core.publisher.Mono;
import services.lootSplitter.model.SplitLootModel;
import services.lootSplitter.model.SplittingMember;
import services.lootSplitter.model.TransferData;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static discord.Connector.client;

public class SplitterTransfersHandler extends InteractionEvent {
    private final int interactionTimeoutDays = 2;

    protected SplitterTransfersHandler() {
        super("transfer");
    }

    @Override
    public void executeEvent() {
        client.on(ButtonInteractionEvent.class, event -> {
            if (!event.getCustomId().contains(getButtonId())) return Mono.empty();
            Message message = getMessage(event);
            String id = getId(event);
            String splitterName = id.split("_")[1];

            List<ComponentData> interactionButtons = getInteractionButtons(message);
            boolean isTimeout = isTimeout(event);
            message.edit()
                    .withComponents(ActionRow.of(updateButtons(interactionButtons, id, splitterName, isTimeout)))
                    .subscribe();
            return isTimeout ? event.reply("You cannot send transfer data after " + interactionTimeoutDays + " day(s) from hunt date").withEphemeral(true) :
                    event.reply(buildSplittingResponse(id, splitterName));
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
        return getButtonId() + "_" + buttonId;
    }

    private List<Button> updateButtons(List<ComponentData> interactionButtons, String customId, String splitterName, boolean isTimeout) {
        List<Button> buttons = new ArrayList<>();
        for (ComponentData data : interactionButtons) {
            if(data.customId().get().equals(customId)) {
                String text = splitterName + (isTimeout ? " transfer(s) timed out" : " sent transfers");
                buttons.add(Button.primary(data.customId().get(), text).disabled());
                continue;
            }

            buttons.add((Button) Button.fromData(data));
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

    private String buildSplittingResponse(String customId, String splitterName) {
        StringBuilder builder = new StringBuilder();
        splitInteractionIdToTransfers(customId).forEach((k, v) -> builder
                .append("Very well. ")
                .append(splitterName)
                .append(" has transferred ")
                .append(v)
                .append(" gold to ")
                .append(k)
                .append("\n"));
        return builder.toString();
    }

    private boolean isTimeout(ButtonInteractionEvent event) {
        Instant msgDate = event.getMessage().get().getTimestamp();
        return msgDate.plus(Duration.ofDays(interactionTimeoutDays))
                .isBefore(LocalDateTime.now().toInstant(ZoneOffset.UTC));
    }

    @Override
    public String getEventName() {
        return "";
    }

    @Override
    protected void executeEventProcess() {

    }
}
