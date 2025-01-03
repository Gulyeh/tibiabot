package events.lootSplitter;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionPresentModalSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.discordjson.json.ComponentData;
import discord4j.rest.util.Color;
import events.abstracts.EmbeddableEvent;
import events.utils.EventName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import services.lootSplitter.LootSplitterService;
import services.lootSplitter.model.SplitLootModel;
import services.lootSplitter.model.SplittingMember;
import services.lootSplitter.model.TransferData;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static builders.commands.names.CommandsNames.splitLootCommand;
import static discord.Connector.client;
import static discord.messages.GetMessages.getChannelMessages;

public class LootSplitter extends EmbeddableEvent {
    private final LootSplitterService service;
    private final SplitterInteractionsHandler splitterInteractionsHandler;
    private final String splitModalId = "lootSplitterModal";
    private final String spotModalId = "spotModal";
    private final String splitterModalId = "splitterModal";

    public LootSplitter() {
        service = new LootSplitterService();
        splitterInteractionsHandler = new SplitterInteractionsHandler();
    }

    private void subscribeCommandEvent() {
        client.on(ChatInputInteractionEvent.class, event -> {
            try {
                if (!event.getCommandName().equals(splitLootCommand)) return Mono.empty();
                return event.presentModal(InteractionPresentModalSpec.builder()
                        .title("Loot Splitter")
                        .customId(splitModalId)
                        .addAllComponents(Arrays.asList(
                                ActionRow.of(TextInput.small(spotModalId, "Spot name")
                                        .placeholder("Hunt spot").required(false)),
                                ActionRow.of(TextInput.paragraph(splitterModalId, "Session data")
                                        .placeholder("Party hunt analyzer").required(true))))
                        .build());
            } catch (Exception e) {
                logINFO.error(e.getMessage());
                return Mono.empty();
            }
        }).subscribe();
    }

    private void subscribeModalEvent() {
        client.on(ModalSubmitInteractionEvent.class, event -> {
            if (!splitModalId.equals(event.getCustomId())) return Mono.empty();
            String spot = "", analyzer = "";

            for (TextInput component : event.getComponents(TextInput.class)) {
                if (spotModalId.equals(component.getCustomId()))
                    spot = component.getValue().orElse("");
                else if (splitterModalId.equals(component.getCustomId()))
                    analyzer = component.getValue().orElse("");
            }

            SplitLootModel model = service.splitLoot(analyzer, spot);
            analyzer = spot.isEmpty() ? analyzer : "Hunted on: " + spot + "\n\n" + analyzer;
            return event.reply().withFiles(
                            MessageCreateFields.File.of("session.txt",
                                    new ByteArrayInputStream(analyzer.getBytes(StandardCharsets.UTF_8))))
                    .withEmbeds(createMessage(model))
                    .withComponents(ActionRow.of(splitterInteractionsHandler.getSplittingButtons(model)));
        }).subscribe();
    }

    @Override
    public void executeEvent() {
        subscribeCommandEvent();
        subscribeModalEvent();
        splitterInteractionsHandler.executeEvent();
    }

    private List<EmbedCreateSpec> createMessage(SplitLootModel model) {
        String title = "Party Hunt Session - " + model.getMembers().size() + " member(s)";
        if(!model.getSpotName().isEmpty()) title += "\nHunted on " + model.getSpotName();

        Color color = model.isNegative() ? Color.RED : Color.GREEN;
        String footer = model.getHuntTime() + " hunt from " + model.getHuntFrom() + " to " + model.getHuntTo();

        return createEmbeddedMessages(
                createFields(model),
                title,
                "",
                "",
                "",
                color,
                EmbedCreateFields.Footer.of(footer, null)
        );
    }

    private List<EmbedCreateFields.Field> createDescriptionFields(SplitLootModel model) {
        String description = "Type: **" + model.getLootType() +
                "**\nLoot: **" + model.getLoot() +
                "**\nSupplies: **" + model.getSupplies() +
                "**\nBalance: **" + model.getBalance() +
                "**";

        List<EmbedCreateFields.Field> fields = new ArrayList<>();
        fields.add(EmbedCreateFields.Field.of("", description, true));

        description = "\u1CBC\nLoot per hour: **" + model.getLootPerHour() +
                "**\nIndividual balance **" + model.getIndividualBalance() +"**";
        fields.add(EmbedCreateFields.Field.of("", description, true));
        fields.add(EmbedCreateFields.Field.of("\t", "\t", true));
        return fields;
    }

    private EmbedCreateFields.Field createDamageSortField(SplitLootModel model) {
        List<SplittingMember> members = new ArrayList<>(model.getMembers());
        members.sort(Comparator.comparing(SplittingMember::getDamagePercentage).reversed());
        StringBuilder damage = new StringBuilder();
        for(SplittingMember member : members) {
            damage.append("‣ ")
                    .append(member.getName())
                    .append(" (")
                    .append(member.getDamagePercentageString())
                    .append(")\n");
        }
        return EmbedCreateFields.Field.of("Damage", damage.toString(), true);
    }

    private EmbedCreateFields.Field createHealingSortField(SplitLootModel model) {
        List<SplittingMember> members = new ArrayList<>(model.getMembers());
        members.sort(Comparator.comparing(SplittingMember::getHealingPercentage).reversed());
        StringBuilder healing = new StringBuilder();
        for(SplittingMember member : members) {
            healing.append("‣ ")
                    .append(member.getName())
                    .append(" (")
                    .append(member.getHealingPercentageString())
                    .append(")\n");
        }
        return EmbedCreateFields.Field.of("Healing", healing.toString(), true);
    }

    private List<EmbedCreateFields.Field> createFields(SplitLootModel model) {
        StringBuilder splitting = new StringBuilder();
        List<EmbedCreateFields.Field> fields = new ArrayList<>(createDescriptionFields(model));
        fields.add(createDamageSortField(model));
        fields.add(createHealingSortField(model));
        fields.add(emptyField(true));
        fields.add(emptyField(false));
        fields.add(EmbedCreateFields.Field.of("", "**Members**", false));

        int iterations = 1;
        for(SplittingMember member : model.getMembers()) {
            fields.add(buildEmbedField(member));
            if(iterations % 2 == 0) fields.add(emptyField(true));
            iterations++;
            if(member.getTransfers().isEmpty()) continue;

            splitting.append("‣ **")
                    .append(member.getName())
                    .append("**\n");
            for(TransferData transfer : member.getTransfers()) {
                splitting.append("``")
                        .append("transfer ")
                        .append(transfer.getTransferAmount())
                        .append(" to ")
                        .append(transfer.getTransferTo())
                        .append("``\n");
            }
        }

        fields.add(emptyField(false));
        fields.add(EmbedCreateFields.Field.of("**Splitting Instructions**", splitting.toString(), false));
        return fields;
    }

    private EmbedCreateFields.Field buildEmbedField(SplittingMember data) {
        String value = "\u1CBC\u1CBCLoot: **" + data.getLoot() + "** (" + data.getLootPercentageString() + ")" +
                "\n\u1CBC\u1CBCSupplies: **" + data.getSupplies() + "** (" + data.getSuppliesPercentageString() + ")" +
                "\n\u1CBC\u1CBCBalance: **" + data.getBalance() +
                "**\n\u1CBC\u1CBCDamage: **" + data.getDamage() +
                "**\n\u1CBC\u1CBCHealing: **" + data.getHealing() +
                "**";
        return EmbedCreateFields.Field.of("‣ " + data.getName(), value, true);
    }

    private EmbedCreateFields.Field emptyField(boolean inline) {
        return EmbedCreateFields.Field.of("\t", "\t", inline);
    }

    @Override
    public String getEventName() {
        return EventName.getLootSplitter();
    }

    @Override
    protected void executeEventProcess() {
    }
}
